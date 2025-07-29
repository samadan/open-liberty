/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.requests.parsers;

import java.math.BigDecimal;
import java.util.Map;

import io.openliberty.mcp.internal.RequestMethod;
import io.openliberty.mcp.internal.ToolMetadata;
import io.openliberty.mcp.internal.ToolMetadata.ArgumentMetadata;
import io.openliberty.mcp.internal.ToolRegistry;
import io.openliberty.mcp.internal.requests.McpToolCallRequest;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

/**
 *
 */
public class McpToolCallRequestParser implements McpRequestSubParser<McpToolCallRequest> {

    @Override
    public McpToolCallRequest parse(JsonParser parser, String id, RequestMethod method, DeserializationContext ctx) {
        // Expect name + optional arguments
        readStartParams(parser);
        String methodName = readToolName(parser);
        ToolMetadata tool = ToolRegistry.get().getTool(methodName);
        Map<String, ArgumentMetadata> argumentMap = tool.arguments();
        Object[] arguments = readArguments(parser, argumentMap, ctx);
        readEndParams(parser);
        return new McpToolCallRequest(id, method, tool, arguments);
    }

    private void readStartParams(JsonParser parser) {
        Event e = parser.next();
        assert e == Event.KEY_NAME;
        assert parser.getString().equals("params");
        e = parser.next();
        assert e == Event.START_OBJECT;
        // Now at the start of the parameters object
    }

    private void readEndParams(JsonParser parser) {
        Event e = parser.next();
        assert e == Event.END_OBJECT;
    }

    private String readToolName(JsonParser parser) {
        Event e = parser.next();
        assert e == Event.KEY_NAME;
        assert parser.getString().equals("name");
        e = parser.next();
        assert e == Event.VALUE_STRING;
        String methodName = parser.getString();
        return methodName;
    }

    private Object[] readArguments(JsonParser parser, Map<String, ArgumentMetadata> argumentMap, DeserializationContext ctx) {
        Event e = parser.next();
        assert e == Event.KEY_NAME;
        assert parser.getString().equals("arguments");
        e = parser.next();
        assert e == Event.START_OBJECT;
        e = parser.next();
        Object[] result = new Object[argumentMap.size()];
        int argCount = 0;
        while (e == Event.KEY_NAME) {
            String argName = parser.getString();
            ArgumentMetadata data = argumentMap.get(argName);
            if (data == null) {
                throw new RuntimeException("Unknown argument " + argName);
            }
            Object argValue;
            if (data.type() == String.class) {
                argValue = readString(parser);
            } else if (data.type() instanceof Class<?> c && Number.class.isAssignableFrom(c)) {
                argValue = readNumber(parser);
            } else {
                argValue = ctx.deserialize(data.type(), parser);
            }
            result[data.index()] = argValue;
            argCount += 1;
            e = parser.next();
        }
        assert e == Event.END_OBJECT;
        assert argCount == argumentMap.size();
        return result;
    }

    private String readString(JsonParser parser) {
        Event e = parser.next();
        assert e == Event.VALUE_STRING;
        return parser.getString();
    }

    private BigDecimal readNumber(JsonParser parser) {
        Event e = parser.next();
        assert e == Event.VALUE_NUMBER;
        return parser.getBigDecimal();
    }

}
