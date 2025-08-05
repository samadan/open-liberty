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

import io.openliberty.mcp.internal.requests.McpRequest;
import io.openliberty.mcp.internal.requests.McpToolCallParams;
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

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // TODO: validate headers/contentType etc.
        McpRequest request = jsonb.fromJson(req.getInputStream(), McpRequest.class);
        switch (request.getRequestMethod()) {
            case TOOLS_CALL -> callTool(request, resp.getWriter());
            case TOOLS_LIST -> listTools(request, resp.getWriter());
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
        CreationalContext<Void> cc = bm.createCreationalContext(null);
        ToolRegistry toolRegistry = ToolRegistry.get();

        List<ToolDescription> response = new LinkedList();

        if (toolRegistry.hasTools()) {
            for (ToolMetadata tmd : toolRegistry.getAllTools()) {
                response.add(new ToolDescription(tmd));
            }
            jsonb.toJson(response, writer);
        } else {
            // give back an empty response
        }
        // Debug only
        //System.out.println(jsonb.toJson(response));
    }
}
