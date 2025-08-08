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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.internal.Literals;
import io.openliberty.mcp.internal.ToolDescription;
import io.openliberty.mcp.internal.ToolMetadata;
import io.openliberty.mcp.internal.ToolMetadata.ArgumentMetadata;
import io.openliberty.mcp.internal.ToolRegistry;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 *
 */
public class MCPServerToolsListTest {

    ToolRegistry registry = new ToolRegistry();

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        ToolRegistry.set(registry);

        //Weather Tool
        Tool weatherTool = Literals.tool("get_weather", "Weather Information Provider", "Get current weather information for a location");
        Map<String, ArgumentMetadata> arguments = Map.of("location", new ArgumentMetadata(String.class, 0),
                                                         "temperature", new ArgumentMetadata(double.class, 1),
                                                         "humidity", new ArgumentMetadata(int.class, 2));
        registry.addTool(new ToolMetadata(weatherTool, null, null, arguments));

        // Addition Tool
        Tool additionTool = Literals.tool("Addition Calculator", "The Calculator Addition Tool", "Can add two floating point numbers");
        Map<String, ArgumentMetadata> arguments2 = Map.of("number1", new ArgumentMetadata(double.class, 0),
                                                          "number2", new ArgumentMetadata(double.class, 1));
        registry.addTool(new ToolMetadata(additionTool, null, null, arguments2));

        // Subtraction Tool
        Tool subtractionTool = Literals.tool("Subtraction Calculator", "The Calculator Subtraction Tool", "Can subtract two integers");
        Map<String, ArgumentMetadata> arguments3 = Map.of("number1", new ArgumentMetadata(int.class, 0),
                                                          "number2", new ArgumentMetadata(int.class, 1));
        registry.addTool(new ToolMetadata(subtractionTool, null, null, arguments3));

        // True or False Tool
        Tool booleanTool = Literals.tool("Boolean And Operator", "Boolean And Operator", "Does a Boolean And Operation on two boolean variables");
        Map<String, ArgumentMetadata> arguments4 = Map.of("var1", new ArgumentMetadata(boolean.class, 0),
                                                          "var2", new ArgumentMetadata(boolean.class, 1));
        registry.addTool(new ToolMetadata(booleanTool, null, null, arguments4));
    }

    @Test
    public void testJSONSerialization() throws Exception {

        Jsonb jsonb = JsonbBuilder.create();

        List<ToolDescription> response = new LinkedList<>();

        if (registry.hasTools()) {
            for (ToolMetadata tmd : registry.getAllTools()) {
                response.add(new ToolDescription(tmd));
            }
            jsonb.toJson(response);
        }
        String responseString = jsonb.toJson(response);
        String expectedString = """
                        [
                            {
                                "description": "Get current weather information for a location",
                                "inputSchema": {
                                    "properties": {
                                        "temperature": {
                                            "description": "temp desc",
                                            "type": "number"
                                        },
                                        "humidity": {
                                            "description": "temp desc",
                                            "type": "integer"
                                        },
                                        "location": {
                                            "description": "temp desc",
                                            "type": "string"
                                        }
                                    },
                                    "required": [
                                        "temperature",
                                        "humidity",
                                        "location"
                                    ],
                                    "type": "object"
                                },
                                "name": "get_weather",
                                "title": "Weather Information Provider"
                            },
                            {
                                "description": "Does a Boolean And Operation on two boolean variables",
                                "inputSchema": {
                                    "properties": {
                                        "var2": {
                                            "description": "temp desc",
                                            "type": "boolean"
                                        },
                                        "var1": {
                                            "description": "temp desc",
                                            "type": "boolean"
                                        }
                                    },
                                    "required": [
                                        "var2",
                                        "var1"
                                    ],
                                    "type": "object"
                                },
                                "name": "Boolean And Operator",
                                "title": "Boolean And Operator"
                            },
                            {
                                "description": "Can subtract two integers",
                                "inputSchema": {
                                    "properties": {
                                        "number1": {
                                            "description": "temp desc",
                                            "type": "integer"
                                        },
                                        "number2": {
                                            "description": "temp desc",
                                            "type": "integer"
                                        }
                                    },
                                    "required": [
                                        "number1",
                                        "number2"
                                    ],
                                    "type": "object"
                                },
                                "name": "Subtraction Calculator",
                                "title": "The Calculator Subtraction Tool"
                            },
                            {
                                "description": "Can add two floating point numbers",
                                "inputSchema": {
                                    "properties": {
                                        "number2": {
                                            "description": "temp desc",
                                            "type": "number"
                                        },
                                        "number1": {
                                            "description": "temp desc",
                                            "type": "number"
                                        }
                                    },
                                    "required": [
                                        "number1",
                                        "number2"
                                    ],
                                    "type": "object"
                                },
                                "name": "Addition Calculator",
                                "title": "The Calculator Addition Tool"
                            }
                        ]
                        """;

        // Lenient mode test (false boolean in 3rd parameter
        JSONAssert.assertEquals(expectedString, responseString, false);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {}

}
