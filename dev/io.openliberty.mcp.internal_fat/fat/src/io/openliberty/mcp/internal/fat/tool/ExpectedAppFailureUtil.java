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

    /**
     * @param server server the liberty server
     * @param warFileName the name of the warFile used to encapsulate the application
     * @param packageInfo the package for where all of the test classes will be located
     * @throws Exception
     */
    public static void setupAndStartServer(LibertyServer server, String warFileName, Package packageInfo) throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, warFileName).addPackage(packageInfo);
        ShrinkHelper.exportDropinAppToServer(server, war, DISABLE_VALIDATION, SERVER_ONLY);
        server.startServer();

        String APP_NAME = "APP";
        assertThatAppHasFailed(server, APP_NAME);
    }

    /**
     * Asserts that a log message is found indicating that an app has failed to start.
     * This should usually be called before checking for any other validation messages.
     *
     * @param server the liberty server
     * @param appName the app name to search for
     */
    private static void assertThatAppHasFailed(LibertyServer server, String appName) throws Exception {
        /*
         * String logLine = server.waitForStringInLog(APP_STARTED_CODE + "|" + APP_START_FAILED_CODE + ".*" + Pattern.quote(appName)); // Wait until either the app starts or fails
         * to start
         * assertNotNull("No app start messages found for " + appName, logLine);
         * Matcher matcher = Pattern.compile(APP_START_FAILED_CODE).matcher(logLine);
         * assertTrue("App " + appName + " did not fail to start. Found log: " + logLine, matcher.find());
         */

        server.waitForStringInLog(APP_STARTED_CODE + "|" + APP_START_FAILED_CODE); // Wait until either the app start or fails to start
        assertTrue("App was expected to fail!", !server.findStringsInLogs(APP_START_FAILED_CODE).isEmpty());
    }

    /**
     * @param errMsg the error message if the expected error is not found in the logs
     * @param expectedErrorList a list of expected error messages
     * @param server the liberty server
     * @throws Exception
     */
    public static void findAndAssertAllExpectedErrorsInLogs(String errMsg, List<String> expectedErrorList, LibertyServer server) throws Exception {
        for (String err : expectedErrorList) {
            assertTrue(errMsg + " [" + err + "] expected and not found", !server.findStringsInLogs(err).isEmpty());
        }
    }

    public static void stopServerIgnoringErrorMessages(LibertyServer server) throws Exception {
        server.stopServer(APP_START_FAILED_CODE);
    }
}
