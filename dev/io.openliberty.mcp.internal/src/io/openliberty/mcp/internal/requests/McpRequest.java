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

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import io.openliberty.mcp.internal.RequestMethod;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;

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

    public static McpRequest createValidMCPRequest(Reader re) throws JsonException, JSONRPCException, IOException {
        JsonReader reader = Json.createReader(re);
        JsonObject requestJson = reader.readObject();
        String jsonrpc = requestJson.getString("jsonrpc", null);
        JsonValue id = requestJson.getOrDefault("id", null);
        String method = requestJson.getString("method", null);
        JsonObject params = requestJson.getJsonObject("params");

        ArrayList<String> errors = new ArrayList<>();

        if (jsonrpc == null || !jsonrpc.equals("2.0"))
            errors.add("jsonrpc field must be present. Only JSONRPC 2.0 is currently supported");
        if (id.getValueType().equals(JsonValue.ValueType.NULL) || !(id.getValueType().equals(JsonValue.ValueType.STRING)
                                                                    || (id.getValueType().equals(JsonValue.ValueType.NUMBER))))
            errors.add("id must be a string or number");
        if (id.getValueType().equals(JsonValue.ValueType.STRING) && ((JsonString) id).getString().isBlank())
            errors.add("id must not be empty");
        if (method == null || method.isBlank())
            errors.add("method must be present and not empty");
        if (params == null)
            errors.add("params field must be present and not empty");

        if (!errors.isEmpty())
            throw new JSONRPCException(JSONRPCErrorCode.INVALID_REQUEST, errors);
        Object idObj = null;

        if (id.getValueType().equals(JsonValue.ValueType.STRING)) {
            idObj = ((JsonString) id).getString();
        } else if (id.getValueType().equals(JsonValue.ValueType.NUMBER)) {
            idObj = ((JsonNumber) id).numberValue();
        }

        return new McpRequest(jsonrpc, idObj, method, params);
    }

}
