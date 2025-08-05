/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 *
 */
@RunWith(FATRunner.class)
public class ToolTest extends FATServletClient {

    @Server("mcp-server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "toolTest.war")
                                   .addPackage(BasicTools.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testEchoWithNumberIdType() throws Exception {
        String request = """
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
                        """;

        String response = new HttpRequest(server, "/toolTest/mcp").jsonBody(request).method("POST").run(String.class);
        JSONObject jsonResponse = new JSONObject(response);

        // Lenient mode tests
        JSONAssert.assertEquals("{ \"jsonrpc\": \"2.0\", \"id\": 2}", response, false);
        JSONAssert.assertEquals("{\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"Hello\"}]}}", jsonResponse, false);

        // Strict Mode tests
        String expectedResponseString = """
                        {"id":2,"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello"}]}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testEchoWithStringIdType() throws Exception {
        String request = """
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
                        """;

        String response = new HttpRequest(server, "/toolTest/mcp").jsonBody(request).method("POST").run(String.class);
        JSONObject jsonResponse = new JSONObject(response);

        // Lenient mode tests
        JSONAssert.assertEquals("{ \"jsonrpc\": \"2.0\", \"id\": \"2\"}", response, false);
        JSONAssert.assertEquals("{\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"Hello\"}]}}", jsonResponse, false);

        // Strict Mode tests
        String expectedResponseString = """
                        {"id":\"2\","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello"}]}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolList() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/list",
                          "params": {
                            "cursor": "optional-cursor-value"
                          }
                        }
                        """;

        String response = new HttpRequest(server, "/toolTest/mcp").jsonBody(request).method("POST").run(String.class);
        JSONObject jsonResponse = new JSONObject(response);
        Jsonb jsonb = JsonbBuilder.create();
        String responseString = jsonb.toJson(jsonResponse);

        // Lenient mode tests
        //JSONAssert.assertEquals("{ \"jsonrpc\": \"2.0\", \"id\": 1}", response, false);
        //JSONAssert.assertEquals("{\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"Hello\"}]}}", jsonResponse, false);

        // Strict Mode tests
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
        JSONAssert.assertEquals(expectedString, response, false);
    }

}
