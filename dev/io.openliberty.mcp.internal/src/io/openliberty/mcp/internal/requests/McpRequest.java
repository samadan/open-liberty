/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.requests;

import io.openliberty.mcp.internal.RequestMethod;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;

public record McpRequest(String jsonrpc,
                         Object id,
                         String method,
                         JsonObject params) {
    public McpRequest {
        if (jsonrpc == null || !jsonrpc.equals("2.0"))
            throw new IllegalArgumentException("jsonrpc field must be present. Only JSONRPC 2.0 is currently supported");
        if (id == null || !(id instanceof String || id instanceof Number))
            throw new IllegalArgumentException("id must be a string or number");
        if (id instanceof String && ((String) id).isBlank())
            throw new IllegalArgumentException("id must not be empty");
        if (method == null || method.isBlank())
            throw new IllegalArgumentException("method must be present and not empty");
        if (params == null)
            throw new IllegalArgumentException("Params field must be present");

    }

//    Returns the enum value of the supported tool methods
    /**
     * Converts the string value of the method from an MCP request into a matching enum value
     *
     * @return the matching {@link RequestMethod} enum value
     */
    public RequestMethod getRequestMethod() {
        return RequestMethod.getForMethodName(method);
    }

    /**
     * Deserialises the MCP request params value from JSON into an object of the specified type
     *
     * @param <T> the target type to map the JSON into
     * @param type the class we want to deserialise the JSON into
     * @param jsonb the jsonb deserialiser to convert the JSON string into an object
     * @return
     */
    public <T> T getParams(Class<T> type, Jsonb jsonb) {
        String json = jsonb.toJson(params);
        return jsonb.fromJson(json, type);
    }
}
