/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.responses;

/**
 * An MCP Response message
 *
 */
public abstract class McpResponse {
    String jsonrpc;
    Object id;

    public McpResponse(String jsonrpc, Object id) {
        if (jsonrpc == null || !jsonrpc.equals("2.0"))
            throw new IllegalArgumentException("jsonrpc field must be present. Only JSONRPC 2.0 is currently supported");
        if (id == null || !(id instanceof String || id instanceof Number))
            throw new IllegalArgumentException("id must be a string or number");

        this.jsonrpc = jsonrpc;
        this.id = id;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public Object getId() {
        return id;
    }
}
