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

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import io.openliberty.mcp.internal.Capabilities.ServerCapabilities;
import io.openliberty.mcp.internal.requests.McpInitializeParams;
import io.openliberty.mcp.internal.requests.McpRequest;
import io.openliberty.mcp.internal.requests.McpToolCallParams;
import io.openliberty.mcp.internal.responses.McpInitializeResult;
import io.openliberty.mcp.internal.responses.McpInitializeResult.ServerInfo;
import io.openliberty.mcp.internal.responses.McpResponse;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
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
        // TODO Set up notification listening
        super.doGet(req, resp);
    }

    // Helper method
    private boolean acceptContains(String acceptHeader, String mime) {
        if (acceptHeader == null)
            return false;
        String target = mime.toLowerCase(java.util.Locale.ROOT);
        for (String part : acceptHeader.split(",")) {
            String value = part.trim().toLowerCase(java.util.Locale.ROOT);
            if (value.startsWith(target))
                return true;
        }
        return false;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String accept = req.getHeader("Accept");
        if (accept == null || !HeaderValidation.acceptContains(accept, "application/json") || !HeaderValidation.acceptContains(accept, "text/event-stream")) {
            resp.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            resp.setContentType("application/json");
            return;
        } ;

        McpRequest request = jsonb.fromJson(req.getInputStream(), McpRequest.class);
        switch (request.getRequestMethod()) {
            case TOOLS_CALL -> callTool(request, resp.getWriter());
            case TOOLS_LIST -> listTools(request, resp.getWriter());
            case INITIALIZE -> initialize(request, resp.getWriter());
            default -> throw new IllegalArgumentException("Unexpected value: " + request.getRequestMethod());
        }
    }

    /**
     * @param request
     * @return
     */
    private void callTool(McpRequest request, Writer writer) {
        McpToolCallParams params = request.getParams(McpToolCallParams.class, jsonb);
        CreationalContext<Void> cc = bm.createCreationalContext(null);
        Object bean = bm.getReference(params.getBean(), params.getBean().getBeanClass(), cc);
        ToolResponse response;
        try {
            Object result = params.getMethod().invoke(bean, params.getArguments(jsonb));
            response = ToolResponse.createFor(request.id(), result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        jsonb.toJson(response, writer);
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
            McpResponse mcpResponse = new McpResponse(request.id(), toolResult);
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
        McpResponse response = new McpResponse(request.id(), result);
        jsonb.toJson(response, writer);
    }

}
