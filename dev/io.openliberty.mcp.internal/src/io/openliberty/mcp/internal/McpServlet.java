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

import io.openliberty.mcp.internal.requests.McpRequest;
import io.openliberty.mcp.internal.requests.McpToolCallRequest;
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
            case TOOLS_CALL -> callTool((McpToolCallRequest) request, resp.getWriter());
            default -> throw new IllegalArgumentException("Unexpected value: " + request.getRequestMethod());
        }
    }

    /**
     * @param request
     * @return
     */
    private void callTool(McpToolCallRequest request, Writer writer) {
        CreationalContext<Void> cc = bm.createCreationalContext(null);
        Object bean = bm.getReference(request.getBean(), request.getBean().getBeanClass(), cc);
        ToolResponse response;
        try {
            Object result = request.getMethod().invoke(bean, request.getArguments());
            response = ToolResponse.createFor(request.getId(), result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        jsonb.toJson(response, writer);
    }

}
