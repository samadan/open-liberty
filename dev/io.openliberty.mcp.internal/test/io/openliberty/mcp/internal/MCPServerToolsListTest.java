/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.internal.ToolMetadata.ArgumentMetadata;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 *
 */
public class MCPServerToolsListTest {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {}

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testJSONSerialization() throws Exception {

        ToolRegistry registry = new ToolRegistry();
        ToolRegistry.set(registry);
        Tool testTool = Literals.tool("get_weather", "Weather Information Provider", "Get current weather information for a location");
        Map<String, ArgumentMetadata> arguments = Map.of("location", new ArgumentMetadata(String.class, 0),
                                                         "temperature", new ArgumentMetadata(double.class, 1),
                                                         "humidity", new ArgumentMetadata(int.class, 2));

        registry.addTool(new ToolMetadata(testTool, null, null, arguments));

        Jsonb jsonb = JsonbBuilder.create();

        List<ToolDescription> response = new LinkedList();

        if (registry.hasTools()) {
            for (ToolMetadata tmd : registry.getAllTools()) {
                response.add(new ToolDescription(tmd));
            }
            jsonb.toJson(response);
        }
        System.out.println(jsonb.toJson(response));
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {}

}
