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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

    private static final String ACCEPT_HEADER = "application/json, text/event-stream";
    private static final String MCP_PROTOCOL_HEADER = "MCP-Protocol-Version";
    private static final String MCP_PROTOCOL_VERSION = "2025-06-18";

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

    private static final String ENDPOINT = "/toolTest/mcp";

    @Test
    public void testGetRequestWithoutAcceptHeaderReturns405() throws Exception {
        HttpRequest request = new HttpRequest(server, ENDPOINT)
                                                               .method("GET")
                                                               .expectCode(405);

        String response = request.run(String.class);

        assertNotNull("Expected response body for 405 error", response);
        assertEquals("GET method not allowed.", response);
    }

    @Test
    public void testGetRequestWithTextEventStreamReturns405() throws Exception {
        HttpRequest request = new HttpRequest(server, ENDPOINT)
                                                               .requestProp("Accept", "text/event-stream")
                                                               .method("GET")
                                                               .expectCode(405);

        String response = request.run(String.class);

        assertNotNull("Expected response body for 405 error", response);
        assertEquals("GET not supported yet. SSE not implemented.", response);
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

        HttpRequest JsonRequest = new HttpRequest(server, "/toolTest/mcp").requestProp(MCP_PROTOCOL_VERSION, MCP_PROTOCOL_HEADER).jsonBody(request).method("POST").expectCode(406);

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

        HttpRequest JsonRequest = new HttpRequest(server, "/toolTest/mcp").requestProp("Accept", "application/json").requestProp(MCP_PROTOCOL_VERSION, MCP_PROTOCOL_HEADER)
                                                                          .jsonBody(request).method("POST").expectCode(406);

        String response = JsonRequest.run(String.class);
        assertNull("Expected no response body for 406 Not Acceptable due to incorrect Accept header", response);
    }

    @Test
    public void testMissingMcpProtocolVersionHeader() throws Exception {
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
        String response = HttpTestUtils.callMCPWithoutProtocolVersion(server, "/toolTest", request);
        assertTrue("Expected 400 error body mentioning MCP-Protocol-Version", response.contains("Missing or invalid MCP-Protocol-Version header"));
    }

    @Test
    public void testInitializeDoesNotNeedMcpProtocolVersionHeader() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "initialize",
                         "params": {
                            "clientInfo": {
                              "name": "test-client",
                              "version": "0.1"
                            },
                            "rootUri": "file:/test/root"
                          }
                        }
                        """;

        String response = new HttpRequest(server, "/toolTest/mcp")
                                                                  .requestProp("Accept", ACCEPT_HEADER)
                                                                  .jsonBody(request)
                                                                  .method("POST")
                                                                  .expectCode(200)
                                                                  .run(String.class);

        assertTrue("Expected response to contain result", response.contains("\"result\""));
        assertTrue("Expected protocolVersion field in response", response.contains("\"protocolVersion\""));
        assertTrue("Expected serverInfo field in response", response.contains("\"serverInfo\""));
    }

    @Test
    public void testRejectsUnsupportedProtocolVersion() throws Exception {
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

        String response = new HttpRequest(server, "/toolTest/mcp")
                                                                  .requestProp("Accept", ACCEPT_HEADER)
                                                                  .requestProp("MCP-Protocol-Version", "2022-02-02")
                                                                  .jsonBody(request)
                                                                  .method("POST")
                                                                  .expectCode(400)
                                                                  .run(String.class);

        assertTrue("Expected error message about invalid protocol version", response.contains("Missing or invalid MCP-Protocol-Version header"));
        assertTrue("Expected error message to contain expected version", response.contains("Expected: 2025-06-18"));
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
                            "method must be present and not empty",
                            "id must be a string or number"
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
                            "args [other] passed but not found in method",
                            "args [input] were expected by the method"
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
                            "result": {
                                "tools": [
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "first number",
                                                    "type": "integer"
                                                },
                                                "num2": {
                                                    "description": "second number",
                                                    "type": "integer"
                                                }
                                            },
                                            "required": [
                                                "num1",
                                                "num2"
                                            ]
                                        },
                                        "name": "add",
                                        "description": "Returns the sum of the two inputs",
                                        "title": "Addition calculator"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "input": {
                                                    "description": "input to echo",
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "input"
                                            ]
                                        },
                                        "name": "privateEcho",
                                        "description": "Returns the input unchanged",
                                        "title": "Echoes the input"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "type": "integer"
                                                },
                                                "num2": {
                                                    "type": "integer"
                                                }
                                            },
                                            "required": [
                                                "num1",
                                                "num2"
                                            ]
                                        },
                                        "name": "subtract",
                                        "description": "Minus number 2 from number 1",
                                        "title": "Subtraction calculator"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "input": {
                                                    "description": "input to echo",
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "input"
                                            ]
                                        },
                                        "name": "echo",
                                        "description": "Returns the input unchanged",
                                        "title": "Echoes the input"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "long",
                                                    "type": "number"
                                                }
                                            },
                                            "required": [
                                                "num1"
                                            ]
                                        },
                                        "name": "testJSONlong",
                                        "description": "testJSONlong",
                                        "title": "testJSONlong"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "double",
                                                    "type": "number"
                                                }
                                            },
                                            "required": [
                                                "num1"
                                            ]
                                        },
                                        "name": "testJSONdouble",
                                        "description": "testJSONdouble",
                                        "title": "testJSONdouble"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "byte",
                                                    "type": "number"
                                                }
                                            },
                                            "required": [
                                                "num1"
                                            ]
                                        },
                                        "name": "testJSONbyte",
                                        "description": "testJSONbyte",
                                        "title": "testJSONbyte"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "float",
                                                    "type": "number"
                                                }
                                            },
                                            "required": [
                                                "num1"
                                            ]
                                        },
                                        "name": "testJSONfloat",
                                        "description": "testJSONfloat",
                                        "title": "testJSONfloat"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "short",
                                                    "type": "number"
                                                }
                                            },
                                            "required": [
                                                "num1"
                                            ]
                                        },
                                        "name": "testJSONshort",
                                        "description": "testJSONshort",
                                        "title": "testJSONshort"
                                    },{
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "Long",
                                                    "type": "number"
                                                }
                                            },
                                            "required": [
                                                "num1"
                                            ]
                                        },
                                        "name": "testJSONLong",
                                        "description": "testJSONLong",
                                        "title": "testJSONLong"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "Double",
                                                    "type": "number"
                                                }
                                            },
                                            "required": [
                                                "num1"
                                            ]
                                        },
                                        "name": "testJSONDouble",
                                        "description": "testJSONDouble",
                                        "title": "testJSONDouble"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "Byte",
                                                    "type": "number"
                                                }
                                            },
                                            "required": [
                                                "num1"
                                            ]
                                        },
                                        "name": "testJSONByte",
                                        "description": "testJSONByte",
                                        "title": "testJSONByte"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "Float",
                                                    "type": "number"
                                                }
                                            },
                                            "required": [
                                                "num1"
                                            ]
                                        },
                                        "name": "testJSONFloat",
                                        "description": "testJSONFloat",
                                        "title": "testJSONFloat"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "Short",
                                                    "type": "number"
                                                }
                                            },
                                            "required": [
                                                "num1"
                                            ]
                                        },
                                        "name": "testJSONShort",
                                        "description": "testJSONShort",
                                        "title": "testJSONShort"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "Integer",
                                                    "type": "integer"
                                                }
                                            },
                                            "required": [
                                                "num1"
                                            ]
                                        },
                                        "name": "testJSONInteger",
                                        "description": "testJSONInteger",
                                        "title": "testJSONInteger"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "c": {
                                                    "description": "Character",
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "c"
                                            ]
                                        },
                                        "name": "testJSONCharacter",
                                        "description": "testJSONCharacter",
                                        "title": "testJSONCharacter"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "c": {
                                                    "description": "char",
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "c"
                                            ]
                                        },
                                        "name": "testJSONcharacter",
                                        "description": "testJSONcharacter",
                                        "title": "testJSONcharacter"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "b": {
                                                    "description": "Boolean",
                                                    "type": "boolean"
                                                }
                                            },
                                            "required": [
                                                "b"
                                            ]
                                        },
                                        "name": "testJSONBoolean",
                                        "description": "testJSONBoolean",
                                        "title": "testJSONBoolean"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "value": {
                                                    "description": "boolean value",
                                                    "type": "boolean"
                                                }
                                            },
                                            "required": [
                                                "value"
                                            ]
                                        },
                                        "name": "toggle",
                                        "description": "toggles the boolean input",
                                        "title": "Boolean toggle"
                                    }
                                ]
                            },
                            "id": 1,
                            "jsonrpc": "2.0"
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
    public void testToolNotFoundError() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "privateEchoMissing",
                            "arguments": {
                              "input": "Hello",
                              "repeat": 4
                            }
                          }
                        }
                        """;

        String response = HttpTestUtils.callMCP(server, "/toolTest", request);
        String expectedResponseString = """
                        {
                            "id": 2,
                            "jsonrpc": "2.0",
                            "error": {
                                "code": -32602,
                                "data": [
                                    "Method privateEchoMissing not found"
                                ],
                                "message": "Invalid params"
                            }
                        }
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

    @Test
    public void testToggleWithBooleanArgs() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "toggle",
                            "arguments": {
                              "value": true
                            }
                          }
                        }
                        """;

        String response = HttpTestUtils.callMCP(server, "/toolTest", request);

        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"type":"text","text": false}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }
}
