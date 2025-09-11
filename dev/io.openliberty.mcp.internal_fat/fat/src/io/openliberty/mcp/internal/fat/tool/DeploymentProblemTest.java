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

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.DuplicateToolErrorTest;

/**
 *
 */
@RunWith(FATRunner.class)
public class DeploymentProblemTest extends FATServletClient {

    @Server("mcp-server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        String warFileName = "DeploymentProblemTest.war";
        Package packageName = DuplicateToolErrorTest.class.getPackage();
        ExpectedAppFailureUtil.setupAndStartServer(server, warFileName, packageName);
    }

    @AfterClass
    public static void teardown() throws Exception {
        ExpectedAppFailureUtil.stopServerIgnoringErrorMessages(server); // ignore app failed to start error
    }

    @Test
    public void testDuplicateToolDeploymentError() throws Exception {
        List<String> expectedErrorList = List.of("io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.DuplicateToolErrorTest.bob",
                                                 "io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.DuplicateToolErrorTest.duplicateBob",
                                                 "io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.DuplicateToolErrorTest2.duplicateBob",
                                                 "io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.DuplicateToolErrorTest.duplicateEcho",
                                                 "io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.DuplicateToolErrorTest.echo",
                                                 "io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.DuplicateToolErrorTest2.duplicateEcho",
                                                 "io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.DuplicateToolErrorTest2.echo");
        ExpectedAppFailureUtil.findAndAssertAllExpectedErrorsInLogs("Duplicate Tool: ", expectedErrorList, server);
    }

    @Test
    public void testToolArgBlankTestCase() throws Exception {
        List<String> expectedErrorList = List.of("io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.ToolArgValidationTest.argNameisBlank",
                                                 "io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.ToolArgValidationTest.argNameisBlankVariant");
        ExpectedAppFailureUtil.findAndAssertAllExpectedErrorsInLogs("Blank Tool Arg: ", expectedErrorList, server);
    }

    @Test
    public void testToolArgDuplicatesTestCase() throws Exception {
        List<String> expectedErrorList = List.of("io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.ToolArgValidationTest.duplicateParam.*arg",
                                                 "io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.ToolArgValidationTest.duplicateParamVariant.*arg");
        ExpectedAppFailureUtil.findAndAssertAllExpectedErrorsInLogs("Duplicate Tool Arg: ", expectedErrorList, server);
    }
}
