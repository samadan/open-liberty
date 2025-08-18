/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.mcp.internal.Capabilities.ServerCapabilities;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.requests.McpInitializeParams;
import io.openliberty.mcp.internal.requests.McpRequest;
import io.openliberty.mcp.internal.requests.McpToolCallParams;
import io.openliberty.mcp.internal.responses.McpErrorResponse;
import io.openliberty.mcp.internal.responses.McpInitializeResult;
import io.openliberty.mcp.internal.responses.McpInitializeResult.ServerInfo;
import io.openliberty.mcp.internal.responses.McpResponse;
import io.openliberty.mcp.internal.responses.McpResultResponse;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.json.JsonException;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 */
public class McpServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(McpServlet.class);

    private Jsonb jsonb;

    @Inject
    BeanManager bm;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        jsonb = JsonbBuilder.create();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String accept = req.getHeader("Accept");

        // Return 405, with SSE-specific message if "text/event-stream" is requested.
        if (accept != null && HeaderValidation.acceptContains(accept, "text/event-stream")) {
            resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            resp.setHeader("Allow", "POST");
            resp.setContentType("text/plain");
            resp.getWriter().write("GET not supported yet. SSE not implemented.");
        } else {
            resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            resp.setHeader("Allow", "POST");
            resp.setContentType("text/plain");
            resp.getWriter().write("GET method not allowed.");
        }
    }

    @Override
    @FFDCIgnore(JSONRPCException.class)
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, JSONRPCException {
        McpRequest request = null;
        try {
            String accept = req.getHeader("Accept");
            if (accept == null || !HeaderValidation.acceptContains(accept, "application/json")
                || !HeaderValidation.acceptContains(accept, "text/event-stream")) {
                resp.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
                resp.setContentType("application/json");
                return;
            } ;
            request = toRequest(req);
            callRequest(request, resp);
        } catch (JSONRPCException e) {
            McpResponse mcpResponse = new McpErrorResponse(request == null ? "" : request.id(), e);
            jsonb.toJson(mcpResponse, resp.getWriter());
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }

    @FFDCIgnore(JsonException.class)
    public McpRequest toRequest(HttpServletRequest req) throws IOException, JSONRPCException {
        McpRequest request = null;
        // TODO: validate headers/contentType etc.
        try {
            BufferedReader re = req.getReader();
            request = McpRequest.createValidMCPRequest(re);
        } catch (JsonbException | JsonException e) {
            throw new JSONRPCException(JSONRPCErrorCode.PARSE_ERROR, List.of(e.getMessage()));
        }
        return request;
    }

    protected void callRequest(McpRequest request, HttpServletResponse resp)
                    throws JSONRPCException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
        switch (request.getRequestMethod()) {
            case TOOLS_CALL -> callTool(request, resp.getWriter());
            case TOOLS_LIST -> listTools(request, resp.getWriter());
            case INITIALIZE -> initialize(request, resp.getWriter());
            case INITIALIZED -> initialized(resp);
            default -> throw new JSONRPCException(JSONRPCErrorCode.METHOD_NOT_FOUND, List.of(String.valueOf(request.getRequestMethod() + " not found")));
        }

    }

    @FFDCIgnore({ JSONRPCException.class, InvocationTargetException.class, IllegalAccessException.class, IllegalArgumentException.class })
    private void callTool(McpRequest request, Writer writer) {
        McpToolCallParams params = request.getParams(McpToolCallParams.class, jsonb);
        CreationalContext<Object> cc = bm.createCreationalContext(null);
        Object bean = bm.getReference(params.getBean(), params.getBean().getBeanClass(), cc);
        McpResponse mcpResponse;
        try {
            Object result = params.getMethod().invoke(bean, params.getArguments(jsonb));
            mcpResponse = new McpResultResponse(request.id(), new ToolResponseResult(result, false));
        } catch (JSONRPCException e) {
            throw e;
        } catch (InvocationTargetException e) {
            mcpResponse = new McpResultResponse(request.id(), new ToolResponseResult(e.getCause().getMessage(), true));;
        } catch (IllegalAccessException e) {
            throw new JSONRPCException(JSONRPCErrorCode.INTERNAL_ERROR, List.of("Could not call " + params.getName()));
        } catch (IllegalArgumentException e) {
            throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS, List.of("Incorrect arguments in params"));
        } finally {
            try {
                cc.release();
            } catch (Exception ex) {
                Tr.warning(tc, "Failed to release bean: " + ex);
            }
        }
        jsonb.toJson(mcpResponse, writer);

    }

    /**
     * @param request
     * @return
     */
    private void listTools(McpRequest request, Writer writer) {
        ToolRegistry toolRegistry = ToolRegistry.get();

        List<ToolDescription> response = new LinkedList<>();

        if (toolRegistry.hasTools()) {
            for (ToolMetadata tmd : toolRegistry.getAllTools()) {
                response.add(new ToolDescription(tmd));
            }
            ToolResult toolResult = new ToolResult(response);
            McpResponse mcpResponse = new McpResultResponse(request.id(), toolResult);
            jsonb.toJson(mcpResponse, writer);
        }
    }

    /**
     * @param request
     * @param writer
     * @return
     */
    private void initialize(McpRequest request, Writer writer) {
        McpInitializeParams params = request.getParams(McpInitializeParams.class, jsonb);
        // TODO validate protocol
        // TODO store client capabilities
        // TODO store client info

        ServerCapabilities caps = ServerCapabilities.of(new Capabilities.Tools(false));

        // TODO: provide a way for the user to set server info
        ServerInfo info = new ServerInfo("test-server", "Test Server", "0.1");
        McpInitializeResult result = new McpInitializeResult("2025-06-18", caps, info, null);
        McpResponse response = new McpResultResponse(request.id(), result);
        jsonb.toJson(response, writer);
    }

    private void initialized(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_ACCEPTED);
    }

}
