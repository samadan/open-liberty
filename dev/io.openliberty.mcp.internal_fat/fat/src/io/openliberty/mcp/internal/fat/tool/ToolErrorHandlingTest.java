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
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BusinessException.ToolErrorHandlingTools;
import io.openliberty.mcp.internal.fat.utils.HttpTestUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class ToolErrorHandlingTest extends FATServletClient {

    private static final String ENDPOINT = "/toolErrorHandlingTest";

    @Server("mcp-server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "toolErrorHandlingTest.war").addPackage(ToolErrorHandlingTools.class.getPackage());
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testToolThrowsToolCallException() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "businessErrorTool",
                            "arguments": {
                              "input": "bad-value"
                            }
                          }
                        }
                        """;

        String response = HttpTestUtils.callMCP(server, ENDPOINT, request);

        String expectedResponseString = """
                        {
                          "id": 1,
                          "jsonrpc": "2.0",
                          "result": {
                            "isError": true,
                            "content": [
                              {
                                "type": "text",
                                "text": "Invalid business input: bad-value"
                              }
                            ]
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolThrowsExceptionWithoutWrapBusiness() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "unwrappedExceptionTool",
                            "arguments": {
                              "input": "trigger"
                            }
                          }
                        }
                        """;

        String response = HttpTestUtils.callMCP(server, ENDPOINT, request);

        String expectedResponse = """
                        {
                          "id": 1,
                          "jsonrpc": "2.0",
                          "result": {
                            "isError": true,
                            "content": [
                              {
                                "type": "text",
                                "text": "Internal server error"
                              }
                            ]
                          }
                        }
                        """;

        JSONAssert.assertEquals(expectedResponse, response, true);
    }

    @Test
    public void testToolThrowsExceptionWithWrapNoArgs() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "wrappedNoArgsTool",
                            "arguments": {
                              "input": "trigger"
                            }
                          }
                        }
                        """;
        String response = HttpTestUtils.callMCP(server, ENDPOINT, request);

        String expectedResponse = """
                        {
                          "id": 1,
                          "jsonrpc": "2.0",
                          "result": {
                            "isError": true,
                            "content": [
                              {
                                "type": "text",
                                "text": "Wrapped error for input: trigger"
                              }
                            ]
                          }
                        }
                        """;

        JSONAssert.assertEquals(expectedResponse, response, true);
    }

    @Test
    public void testToolThrowsListedWrappedException() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "listedWrappedExceptionTool",
                            "arguments": {
                              "input": "bad-value"
                            }
                          }
                        }
                        """;
        String response = HttpTestUtils.callMCP(server, ENDPOINT, request);

        String expectedResponse = """
                        {
                          "id": 1,
                          "jsonrpc": "2.0",
                          "result": {
                            "isError": true,
                            "content": [
                              {
                                "type": "text",
                                "text": "Invalid business input: bad-value"
                              }
                            ]
                          }
                        }
                        """;

        JSONAssert.assertEquals(expectedResponse, response, true);
    }

    @Test
    public void testToolThrowsExceptionWrappedBySuperclass() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "superclassWrappedExceptionTool",
                            "arguments": {
                              "input": "test arg"
                            }
                          }
                        }
                        """;

        String response = HttpTestUtils.callMCP(server, ENDPOINT, request);

        String expectedResponse = """
                        {
                          "id": 1,
                          "jsonrpc": "2.0",
                          "result": {
                            "isError": true,
                            "content": [
                              {
                                "type": "text",
                                "text": "Invalid input for superclass: test arg"
                              }
                            ]
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponse, response, true);
    }

    @Test
    public void testToolThrowsUnwrappedException() throws Exception {
        String request = """
                            {
                              "jsonrpc": "2.0",
                              "id": 1,
                              "method": "tools/call",
                              "params": {
                                "name": "excludeExceptionTool",
                                "arguments": {
                                  "input": "fail-now"
                                }
                              }
                            }
                        """;

        String response = HttpTestUtils.callMCP(server, ENDPOINT, request);

        String expectedResponse = """
                            {
                              "id": 1,
                              "jsonrpc": "2.0",
                              "result": {
                                "isError": true,
                                "content": [
                                  {
                                    "type": "text",
                                    "text": "Internal server error"
                                  }
                                ]
                              }
                            }
                        """;
        JSONAssert.assertEquals(expectedResponse, response, true);
    }

    @Test
    public void testCheckedExceptionTool() throws Exception {
        String request = """
                            {
                              "jsonrpc": "2.0",
                              "id": 1,
                              "method": "tools/call",
                              "params": {
                                "name": "checkedExceptionTool",
                                "arguments": {
                                  "input": "abc"
                                }
                              }
                            }
                        """;

        String response = HttpTestUtils.callMCP(server, ENDPOINT, request);

        String expectedResponse = """
                            {
                              "id": 1,
                              "jsonrpc": "2.0",
                              "result": {
                                "content": [
                                  { "type": "text", "text": "Checked error for: abc" }
                                ],
                                "isError": true
                              }
                            }
                        """;

        JSONAssert.assertEquals(expectedResponse, response, true);
    }

    @Test
    public void testUncheckedExceptionTool() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "uncheckedExceptionTool",
                            "arguments": {
                              "input": "abc"
                            }
                          }
                        }
                        """;

        String response = HttpTestUtils.callMCP(server, ENDPOINT, request);

        String expectedResponse = """
                        {
                          "id": 1,
                          "jsonrpc": "2.0",
                          "result": {
                            "isError": true,
                            "content": [
                              {
                                "type": "text",
                                "text": "Internal server error"
                              }
                            ]
                          }
                        }
                        """;

        JSONAssert.assertEquals(expectedResponse, response, true);
    }
}
