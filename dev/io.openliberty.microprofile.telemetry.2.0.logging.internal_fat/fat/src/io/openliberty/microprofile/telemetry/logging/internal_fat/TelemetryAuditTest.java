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

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

@RunWith(FATRunner.class)
public class TelemetryAuditTest extends FATServletClient {

    public static final String SERVER_NAME = "TelemetryAudit";

    //This test will run on all mp 2.0 repeats to ensure we have some test coverage on all versions.
    //I chose this one because TelemetryMessages is core to this bucket
    // Will re-enable in follow-on issue.
    @ClassRule
    public static RepeatTests rt = TelemetryActions.telemetry20Repeats();

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void initialSetup() throws Exception {
        server.saveServerConfiguration();
    }

    @Before
    public void testSetup() throws Exception {
        server.startServer();
    }

    @After
    public void testTearDown() throws Exception {
        server.stopServer();

        // Restore the server configuration, after each test case.
        server.restoreServerConfiguration();
    }

    /**
     * Ensures Audit messages are correctly bridged and all attributes are present.
     */
    @Test
    public void testTelemetryAuditLogs() throws Exception {
        String line = server.waitForStringInLog("AuditService", server.getConsoleLogFile());

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