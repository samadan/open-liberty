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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.json.bind.adapter.JsonbAdapter;

/**
 *
 */
public class McpResponseAdapter implements JsonbAdapter<McpResponse, JsonObject> {

    /** {@inheritDoc} */
    @Override
    public McpResponse adaptFromJson(JsonObject arg0) throws Exception {
        String jsonrpc = arg0.getString("jsonrpc");
        JsonValue id = arg0.get("id");
        if (arg0.containsKey("result")) {
            return new McpResultResponse(id, arg0.getJsonObject("data"));
        } else if (arg0.containsKey("error")) {
            McpErrorResponse.Error error = new McpErrorResponse.Error(
                                                                      arg0.getJsonObject("error").getInt("code"),
                                                                      arg0.getJsonObject("error").getString("message"),
                                                                      arg0.getJsonObject("error").getJsonObject("data"));
            return new McpErrorResponse(id, error);
        }
        return null;

    }

    /** {@inheritDoc} */
    @Override
    public JsonObject adaptToJson(McpResponse arg0) throws Exception {
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        jsonBuilder.add("jsonrpc", arg0.getJsonrpc());
        jsonBuilder.add("id", (JsonValue) arg0.getId());
        if (arg0 instanceof McpResultResponse) {
            jsonBuilder.add("result", (JsonObject) ((McpResultResponse) arg0).getResult());
        } else if (arg0 instanceof McpErrorResponse) {
            jsonBuilder.add("error", (JsonObject) ((McpErrorResponse) arg0).getError());
        }

        return jsonBuilder.build();
    }

}
