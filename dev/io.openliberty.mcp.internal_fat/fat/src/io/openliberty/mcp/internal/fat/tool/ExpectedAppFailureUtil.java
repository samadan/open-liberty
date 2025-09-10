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

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;

public class ExpectedAppFailureUtil {

    private static String APP_STARTED_CODE = "CWWKZ000[13]I";
    private static String APP_START_FAILED_CODE = "CWWKZ000[24]E";

    public static void setupAndStartServer(LibertyServer server, String warFileName, Package packageName) throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, warFileName).addPackage(packageName);
        ShrinkHelper.exportDropinAppToServer(server, war, DISABLE_VALIDATION, SERVER_ONLY);
        server.startServer();
    }

    public static boolean appHasFailed(LibertyServer server) throws Exception {
        // This needs to be called as the first test method in the test FAT class
        server.waitForStringInLog(APP_STARTED_CODE + "|" + APP_START_FAILED_CODE); // Wait until either the app start or fails to start
        return !server.findStringsInLogs(APP_START_FAILED_CODE).isEmpty();
    }

    public static void findAndAssertAllExpectedErrorsInLogs(String errMsg, List<String> expectedErrorList, LibertyServer server) throws Exception {
        for (String err : expectedErrorList) {
            assertTrue(errMsg + " [" + err + "] expected and not found", !server.findStringsInLogs(err).isEmpty());
        }
    }

    public static void stopServerIgnoringErrorMessages(LibertyServer server) throws Exception {
        server.stopServer(APP_START_FAILED_CODE);
    }
}
