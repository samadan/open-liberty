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
import io.openliberty.mcp.internal.exceptions.jsonrpc.MCPRequestValidationException;
import jakarta.json.*;
import jakarta.json.bind.Jsonb;

import java.io.Reader;

public record McpRequest(String jsonrpc,
                         Object id,
                         String method,
                         JsonObject params) {

//    Returns the enum value of the supported tool methods
    /**
     * Converts the string value of the method from an MCP request into a matching enum value
     *
     * @return the matching {@link RequestMethod} enum value
     */
    public RequestMethod getRequestMethod() {
        return RequestMethod.getForMethodName(this.method);
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
        String json = jsonb.toJson(this.params);
        return jsonb.fromJson(json, type);
    }

    public static McpRequest createValidMCPRequest(Reader reader) throws JsonException, MCPRequestValidationException {

        JsonObject requestJson = Json.createReader(reader).readObject();

        String jsonRpc = requestJson.getString("jsonrpc", null);
        JsonValue id = requestJson.getOrDefault("id", null);
        String method = requestJson.getString("method", null);
        JsonObject params = requestJson.getJsonObject("params");

        validateJsonRpc(jsonRpc);
        validateMethod(method);

        if (id == null) {
            return createMCPNotificationRequest(jsonRpc, method, params);
        }

        Object idObj = parseAndValidateId(id);

        return new McpRequest(jsonRpc, idObj, method, params);
    }

    private static McpRequest createMCPNotificationRequest(String jsonRpc,
                                                           String method,
                                                           JsonObject params) {
        return new McpRequest(jsonRpc, null, method, params);
    }

    private static void validateJsonRpc(String jsonRpc) throws MCPRequestValidationException {
        if (!"2.0".equals(jsonRpc)) {
            throw new MCPRequestValidationException("jsonrpc field must be present. Only JSONRPC 2.0 is currently supported");
        }
    }

    private static void validateMethod(String method) throws MCPRequestValidationException {
        if (method == null || method.isBlank()) {
            throw new MCPRequestValidationException("method must be present and not empty");
        }
    }

    private static Object parseAndValidateId(JsonValue id) throws MCPRequestValidationException {
        JsonValue.ValueType idValueType = id.getValueType();

        if (idValueType != JsonValue.ValueType.STRING &&  idValueType != JsonValue.ValueType.NUMBER) {
            throw new MCPRequestValidationException("id must be a string or number");
        }

        if (idValueType == JsonValue.ValueType.STRING) {
            String idString = ((JsonString) id).getString();

            if (idString.isBlank()) {
                throw new MCPRequestValidationException("id must not be empty");
            }
            return idString;
        }

        return ((JsonNumber) id).numberValue();
    }

}
