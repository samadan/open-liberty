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
import static org.junit.Assert.assertNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.HttpTestUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class ToolTest extends FATServletClient {

    @Server("mcp-server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "toolTest.war").addPackage(BasicTools.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testMissingAcceptHeader() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """;

        HttpRequest JsonRequest = new HttpRequest(server, "/toolTest/mcp").jsonBody(request).method("POST").expectCode(406);

        String response = JsonRequest.run(String.class);
        assertNull("Expected no response body for 406 Not Acceptable", response);
    }

    @Test
    public void testIncorrectAcceptHeader() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """;

        HttpRequest JsonRequest = new HttpRequest(server, "/toolTest/mcp").requestProp("Accept", "application/json").jsonBody(request).method("POST").expectCode(406);

        String response = JsonRequest.run(String.class);
        assertNull("Expected no response body for 406 Not Acceptable due to incorrect Accept header", response);
    }

    @Test
    public void postJsonRpc() throws Exception {
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
        String response = HttpTestUtils.callMCP(server, "/toolTest", request);

        JSONObject jsonResponse = new JSONObject(response);
        // Lenient mode tests
        JSONAssert.assertEquals("{ \"jsonrpc\": \"2.0\", \"id\": \"2\"}", response, false);
        JSONAssert.assertEquals("{\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"Hello\"}]}}", jsonResponse, false);

        // Strict Mode tests
        String expectedResponseString = """
                        {"id":\"2\","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
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

        String response = HttpTestUtils.callMCP(server, "/toolTest", request);
        JSONObject jsonResponse = new JSONObject(response);
        // Lenient mode tests
        JSONAssert.assertEquals("{ \"jsonrpc\": \"2.0\", \"id\": 2}", response, false);
        JSONAssert.assertEquals("{\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"Hello\"}]}}", jsonResponse, false);

        // Strict Mode tests
        String expectedResponseString = """
                        {"id":2,"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello"}], "isError": false}}
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

        String response = HttpTestUtils.callMCP(server, "/toolTest", request);
        JSONObject jsonResponse = new JSONObject(response);

        // Lenient mode tests
        JSONAssert.assertEquals("{ \"jsonrpc\": \"2.0\", \"id\": \"2\"}", response, false);
        JSONAssert.assertEquals("{\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"Hello\"}]}}", jsonResponse, false);

        // Strict Mode tests
        String expectedResponseString = """
                        {"id":\"2\","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testEchoWithInvalidRequestException() throws Exception {
        String request = """
                          {
                          "jsonrpc": "1.0",
                          "id": false
                        }
                        """;

        String response = HttpTestUtils.callMCP(server, "/toolTest", request);
        String expectedResponseString = """
                        {"error":{"code":-32600,
                        "data":[
                            "jsonrpc field must be present. Only JSONRPC 2.0 is currently supported",
                            "id must be a string or number",
                            "method must be present and not empty",
                            "params field must be present and not empty"
                            ],
                        "message":"Invalid request"},
                        "id":"",
                        "jsonrpc":"2.0"}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testEchoWithParseErrorException() throws Exception {
        String request = """
                          }
                          "jsonrpc": "1.0",
                          "id": false,
                          {
                        }
                        """;

        String response = HttpTestUtils.callMCP(server, "/toolTest", request);
        String expectedResponseString = """
                        {"error":{"code":-32700,
                        "message":"Parse error",
                        "data":["Invalid token=CURLYCLOSE at (line no=1, column no=3, offset=2). Expected tokens are: [CURLYOPEN, SQUAREOPEN, STRING, NUMBER, TRUE, FALSE, NULL]"]},
                        "id":"",
                        "jsonrpc":"2.0"}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, false);
    }

    @Test
    public void testEchoWithInvalidParamsException() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "echo"
                          }
                        }
                        """;

        String response = HttpTestUtils.callMCP(server, "/toolTest", request);
        String expectedResponseString = """
                        {"error":{"code":-32602,
                        "data":[
                            "Missing arguments in params"
                            ],
                        "message":"Invalid params"},
                        "id":"2",
                        "jsonrpc":"2.0"}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testEchoWithInvalidParamsArgumentMismatchException() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                                "other": "Hello"
                              }
                          }
                        }
                        """;

        String response = HttpTestUtils.callMCP(server, "/toolTest", request);
        String expectedResponseString = """
                        {"error":{"code":-32602,
                        "data":[
                            "0 arguments processed, expected 1"
                            ],
                        "message": "Invalid params"},
                        "id":"2",
                        "jsonrpc":"2.0"}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testEchoWithMethodNotFoundException() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "call/tools",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """;

        String response = HttpTestUtils.callMCP(server, "/toolTest", request);
        String expectedResponseString = """
                        {"error":{"code":-32601,
                        "data":[
                            "call/tools not found"
                            ],
                        "message":"Method not found"},
                        "id":"2",
                        "jsonrpc":"2.0"}
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

        String response = HttpTestUtils.callMCP(server, "/toolTest", request);
        JSONObject jsonResponse = new JSONObject(response);
        String expectedString = """
                                {
                                    "id": 1,
                                    "jsonrpc": "2.0",
                                    "result": {
                                        "tools": [
                                            {
                                                "name": "add",
                                                "description": "Returns the sum of the two inputs",
                                                "title": "Addition calculator",
                                                "inputSchema": {
                                                    "type": "object",
                                                    "properties": {
                                                        "num1": {
                                                            "description": "temp desc",
                                                            "type": "integer"
                                                        },
                                                        "num2": {
                                                            "description": "temp desc",
                                                            "type": "integer"
                                                        }
                                                    },
                                                    "required": [
                                                        "num1",
                                                        "num2"
                                                    ]
                                                }
                                            },
                                            {
                                                "name": "echo",
                                                "description": "Returns the input unchanged",
                                                "title": "Echoes the input",
                                                "inputSchema": {
                                                    "type": "object",
                                                    "properties": {
                                                        "input": {
                                                            "description": "temp desc",
                                                            "type": "string"
                                                        }
                                                    },
                                                    "required": [
                                                        "input"
                                                    ]
                                                }
                                            },
                                            {
                                                "name": "privateEcho",
                                                "description": "Returns the input unchanged",
                                                "title": "Echoes the input",
                                                "inputSchema": {
                                                    "type": "object",
                                                    "properties": {
                                                        "input": {
                                                            "description": "temp desc",
                                                            "type": "string"
                                                        }
                                                    },
                                                    "required": [
                                                        "input"
                                                    ]
                                                }
                                            }
                                        ]
                                    }
                                }
                        """;

        // Lenient mode test (false boolean in 3rd parameter
        JSONAssert.assertEquals(expectedString, jsonResponse.toString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testEchoMethodCallError() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "throw error"
                            }
                          }
                        }
                        """;

        String response = HttpTestUtils.callMCP(server, "/toolTest", request);
        JSONObject jsonResponse = new JSONObject(response);

        String expectedResponseString = """
                        {"id":2,"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Method call caused runtime exception"}], "isError": true}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testprivateMethodAccessError() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "privateEcho",
                            "arguments": {
                              "input": "throw error"
                            }
                          }
                        }
                        """;

        String response = HttpTestUtils.callMCP(server, "/toolTest", request);
        JSONObject jsonResponse = new JSONObject(response);

        String expectedResponseString = """
                        {"error":{"code":-32603,
                        "data":[
                            "Could not call privateEcho"
                            ],
                        "message":"Internal error"},
                        "id":2,
                        "jsonrpc":"2.0"}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testAddWithIntegerInputArgs() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "add",
                            "arguments": {
                              "num1": 100,
                              "num2": 200
                            }
                          }
                        }
                        """;

        String response = HttpTestUtils.callMCP(server, "/toolTest", request);
        JSONObject jsonResponse = new JSONObject(response);

        // Lenient mode tests
        JSONAssert.assertEquals("{\"result\":{\"content\":[{\"type\":\"text\",\"text\": 300}]}}", jsonResponse, false);

        // Strict Mode tests
        String expectedResponseString = """
                        {"id":\"2\","jsonrpc":"2.0","result":{"content":[{"type":"text","text": 300}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }
}
