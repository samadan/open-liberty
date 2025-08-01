/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;

import java.io.StringReader;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.internal.Literals;
import io.openliberty.mcp.internal.RequestMethod;
import io.openliberty.mcp.internal.ToolMetadata;
import io.openliberty.mcp.internal.ToolMetadata.ArgumentMetadata;
import io.openliberty.mcp.internal.ToolRegistry;
import io.openliberty.mcp.internal.requests.McpRequest;
import io.openliberty.mcp.internal.requests.McpToolCallParams;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;

/**
 *
 */
public class MessageParsingTest {

    @BeforeClass
    public static void setup() {
        ToolRegistry registry = new ToolRegistry();
        ToolRegistry.set(registry);
        Tool testTool = Literals.tool("echo", "Echo", "Echos the input");
        Map<String, ArgumentMetadata> arguments = Map.of("input", new ArgumentMetadata(String.class, 0));
        registry.addTool(new ToolMetadata(testTool, null, null, arguments));
    }

    @Test
    public void parseToolCallMethod() {
        Jsonb jsonb = JsonbBuilder.create();
        StringReader reader = new StringReader("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """);
        McpRequest request = jsonb.fromJson(reader, McpRequest.class);
        assertThat(((Number) request.id()).intValue(), equalTo(2));
        assertThat(request.getRequestMethod(), equalTo(RequestMethod.TOOLS_CALL));
        McpToolCallParams toolCallRequest = request.getParams(McpToolCallParams.class, jsonb);
        assertThat(toolCallRequest.getArguments(jsonb), arrayContaining("Hello"));
    }

    @Test
    public void parseStringIdType() {
        Jsonb jsonb = JsonbBuilder.create();
        StringReader reader = new StringReader("""
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """);
        McpRequest request = jsonb.fromJson(reader, McpRequest.class);
        assertThat(request.id(), equalTo("2"));
    }

    @Test(expected = JsonbException.class)
    public void validateFalseIdType() {
        Jsonb jsonb = JsonbBuilder.create();
        StringReader reader = new StringReader("""
                        {
                          "jsonrpc": "2.0",
                          "id": false,
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """);
        jsonb.fromJson(reader, McpRequest.class);
    }

}
