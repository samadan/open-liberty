/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.protocolVersion.tests;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.HttpTestUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class ProtocolVersionTest {
    private static final String ACCEPT_HEADER = "application/json, text/event-stream";

    @Server("mcp-server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "protocolVersionTest.war").addPackage(BasicTools.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
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
        String response = HttpTestUtils.callMCPWithoutProtocolVersion(server, "/protocolVersionTest", request);
        assertTrue("Expected 400 error body mentioning MCP-Protocol-Version",
                   response.contains("Missing or invalid MCP-Protocol-Version header"));
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

        String response = new HttpRequest(server, "/protocolVersionTest/mcp")
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

        String response = new HttpRequest(server, "/protocolVersionTest/mcp")
                                                                             .requestProp("Accept", ACCEPT_HEADER)
                                                                             .requestProp("MCP-Protocol-Version", "2022-02-02")
                                                                             .jsonBody(request)
                                                                             .method("POST")
                                                                             .expectCode(400)
                                                                             .run(String.class);

        assertTrue("Expected error message about invalid protocol version",
                   response.contains("Missing or invalid MCP-Protocol-Version header"));
        assertTrue("Expected error message to contain expected version", response.contains("Expected: 2025-06-18"));
    }

}
