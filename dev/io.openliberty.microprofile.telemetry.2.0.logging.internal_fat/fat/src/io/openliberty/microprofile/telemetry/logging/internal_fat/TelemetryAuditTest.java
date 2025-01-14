/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.logging.internal_fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;

import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.CheckpointRule;
import componenttest.rules.repeater.CheckpointRule.ServerMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

@RunWith(FATRunner.class)
@CheckpointTest(alwaysRun = true)
public class TelemetryAuditTest extends FATServletClient {

    private static Class<?> c = TelemetryAuditTest.class;

    public static final String SERVER_NAME = "TelemetryAudit";

    @ClassRule
    public static CheckpointRule checkpointRule = new CheckpointRule()
                    .setConsoleLogName(TelemetryAuditTest.class.getSimpleName() + ".log")
                    .setServerSetup(TelemetryAuditTest::initialSetup)
                    .setServerStart(TelemetryAuditTest::testSetup)
                    .setServerTearDown(TelemetryAuditTest::testTearDown);

    //This test will run on all mp 2.0 repeats to ensure we have some test coverage on all versions.
    //I chose this one because TelemetryMessages is core to this bucket
    // Will re-enable in follow-on issue.
    @ClassRule
    public static RepeatTests rt = TelemetryActions.telemetry20Repeats();

    private static LibertyServer server;

    public static LibertyServer initialSetup(ServerMode mode) throws Exception {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME, null, true);
        server.saveServerConfiguration();
        return server;
    }

    public static void testSetup(ServerMode mode, LibertyServer server) throws Exception {
        server.startServer();
    }

    public static void testTearDown(ServerMode mode, LibertyServer server) throws Exception {
        server.stopServer();

        // Restore the server configuration, after each test case.
        server.restoreServerConfiguration();
    }

    /**
     * Ensures Audit messages are correctly bridged and all attributes are present.
     */
    @Test
    public void testTelemetryAuditLogs() throws Exception {
        if (CheckpointRule.isActive()) {
            testRestoreTelemetryAuditLogs(server);
        } else {
            testTelemetryAuditLogs(server);
        }
    }

    private void testRestoreTelemetryAuditLogs(LibertyServer s) throws Exception {
        // on restore we expect no audit messages that happened on the checkpoint side
        RemoteFile consoleLog = s.getConsoleLogFile();
        List<String> linesConsoleLog = s.findStringsInLogsUsingMark(".*scopeInfo.*", consoleLog);
        assertEquals("Expected no audit messages on restore", 0, linesConsoleLog.size());

        // generate JMX_MBEAN_REGISTER audit message by hitting the root of the server
        TestUtils.runGetMethod("http://" + s.getHostname() + ":" + s.getHttpDefaultPort());
        String line = s.waitForStringInLog("JMXService", s.getConsoleLogFile());
        assertNotNull("The JMXService audit event was not not found.", line);
    }

    private void testTelemetryAuditLogs(LibertyServer s) throws Exception {
        String line = s.waitForStringInLog("AuditService", s.getConsoleLogFile());

        assertNotNull("The AuditService audit event was not not found.", line);

        Map<String, String> expectedAuditFieldsMap = new HashMap<String, String>() {
            {
                put("io.openliberty.type", "liberty_audit");
                put("io.openliberty.audit.event_name", "SECURITY_AUDIT_MGMT");
                put("io.openliberty.audit.event_sequence_number", "");
                put("io.openliberty.audit.observer.id", "");
                put("io.openliberty.audit.observer.name", "AuditService");

                put("io.openliberty.audit.observer.type_uri", "service/server");

                put("io.openliberty.audit.outcome", "success");

                put("io.openliberty.audit.target.id", "");

                put("io.openliberty.audit.target.type_uri", "service/audit/start");

                put("thread.id", ""); // since, the thread.id can be random, have to make sure the thread.id field is still present.
                put("io.openliberty.sequence", ""); // since, the sequence can be random, have to make sure the thread.id field is still present.
            }
        };

        // Check if the expected key-value pair is correctly formatted and mapped to OTel.
        TestUtils.checkJsonMessage(line, expectedAuditFieldsMap);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

}