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

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.openliberty.mcp.internal.ToolMetadata;
import io.openliberty.mcp.internal.ToolMetadata.ArgumentMetadata;
import io.openliberty.mcp.internal.ToolRegistry;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;

/**
 *
 */
public class McpToolCallParams {

    private Jsonb jsonb;
    private String name;
    private ToolMetadata metadata;
    private JsonObject arguments;
    private Object[] parsedArguments;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        ToolRegistry tools = ToolRegistry.get();
        metadata = tools.getTool(name);
    }

    public void setArguments(JsonObject arguments) {
        this.arguments = arguments;
    }

    public Method getMethod() {
        return metadata.method().getJavaMember();
    }

    public Object[] getArguments(Jsonb jsonb) {
        if (this.arguments == null) {
            throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS, List.of("Missing arguments in params"));
        }
        if (parsedArguments == null) {
            parsedArguments = parseArguments(arguments, jsonb);
        }
        return parsedArguments;
    }

    private Object[] parseArguments(JsonObject arguments2, Jsonb jsonb) {
        JsonObject argsObject = arguments2.asJsonObject();
        Object[] results = new Object[metadata.arguments().size()];
        HashSet<String> argsProcessed = new HashSet<>();
        for (var entry : argsObject.entrySet()) {
            String argName = entry.getKey();
            JsonValue argValue = entry.getValue();
            ArgumentMetadata argMetadata = metadata.arguments().get(argName);
            if (argMetadata != null) {
                String json = jsonb.toJson(argValue);
                results[argMetadata.index()] = jsonb.fromJson(json, argMetadata.type());

            }
            argsProcessed.add(argName);
        }

        if (!argsProcessed.equals(metadata.arguments().keySet())) {
            List<String> data = generateArgumentMismatchData(argsProcessed, metadata.arguments().keySet());
            throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS, data);
        }

        return results;
    }

    public List<String> generateArgumentMismatchData(Set<String> processed, Set<String> expected) {
        Set<String> missing = new HashSet<>(expected);
        Set<String> missingTmp = new HashSet<>(missing);
        Set<String> extra = new HashSet<>(processed);
        missing.removeAll(extra);
        extra.removeAll(missingTmp);
        return List.of("args " + extra + " passed but not found in method", "args " + missing + " were expected by the method");
    }

    public Bean<?> getBean() {
        return metadata.bean();
    }
}
