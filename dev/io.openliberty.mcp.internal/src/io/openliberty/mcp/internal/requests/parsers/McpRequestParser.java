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

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import io.openliberty.mcp.internal.RequestMethod;
import io.openliberty.mcp.internal.requests.McpRequest;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

/**
 *
 */
public class McpRequestParser implements JsonbDeserializer<McpRequest> {

    private static final Map<RequestMethod, McpRequestSubParser<?>> SUB_PARSERS;

    static {
        SUB_PARSERS = new HashMap<>();
        SUB_PARSERS.put(RequestMethod.TOOLS_CALL, new McpToolCallRequestParser());
    }

    @Override
    public McpRequest deserialize(JsonParser parser, DeserializationContext ctx, Type type) {
        readString(parser, "jsonrpc", "2.0");
        String id = readId(parser, "id");
        String method = readString(parser, "method");
        RequestMethod requestMethod = RequestMethod.getForMethodName(method);

        McpRequestSubParser<?> subParser = SUB_PARSERS.get(requestMethod);
        McpRequest request = subParser.parse(parser, id, requestMethod, ctx);

        Event e = parser.next();
        assert e == Event.END_OBJECT;
        assert !parser.hasNext();

        return request;
    }

    private void readString(JsonParser parser, String key, String value) {
        assert readString(parser, key).equals(value);
    }

    private String readString(JsonParser parser, String key) {
        Event e = parser.next();
        assert e == Event.KEY_NAME;
        assert parser.getString().equals(key);
        e = parser.next();
        assert e == Event.VALUE_STRING;
        return parser.getString();
    }

    private String readId(JsonParser parser, String key) {
        Event e = parser.next();
        assert e == Event.KEY_NAME;
        assert parser.getString().equals(key);
        e = parser.next();
        assert e == Event.VALUE_NUMBER;
        return parser.getString();
    }

}
