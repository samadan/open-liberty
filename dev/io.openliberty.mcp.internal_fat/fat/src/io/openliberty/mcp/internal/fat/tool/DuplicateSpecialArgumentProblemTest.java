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

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.DISABLE_VALIDATION;
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
import io.openliberty.mcp.internal.fat.tool.duplicateArgsErrorTestApp.DuplicateArgsErrorTest;

@RunWith(FATRunner.class)
public class DuplicateSpecialArgumentProblemTest {

    @Server("mcp-server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "DuplicateSpecialArgumentProblemTest.war").addPackage(DuplicateArgsErrorTest.class.getPackage());
        ShrinkHelper.exportDropinAppToServer(server, war, DISABLE_VALIDATION, SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("CWWKZ0002E"); // App failed to start
    }

    @Test
    public void testDuplicateArgsDeploymentError() throws Exception {
        String errorInLogs = server.waitForStringInLog("Only 1 instance of type CANCELLATION is allowed", 5 * 1000);

        assertTrue("Expected a deployment error due to duplicate arguments being present: The String `Only 1 instance of type CANCELLATION is allowed` was not present in logs",
                   errorInLogs != null);
    }

}
