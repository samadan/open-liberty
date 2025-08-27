/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test.schema;

import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import io.openliberty.mcp.annotations.SchemaArg;
import io.openliberty.mcp.internal.schemas.SchemaRegistry;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;

/**
 *
 */
public class SchemaTest {
    static SchemaRegistry registry;

    public static record person(@JsonbProperty("fullname") String name, address address, company company) {};

    public static record address(int number, String street, String postcode, @JsonbTransient String directions) {};

    public static record company(String name, address address, List<person> employees,
                                 @SchemaArg(value = "{\"properties\": {\"key\":{ \"type\": \"integer\" }, \"value\":{ \"type\": \"object<person>\" }},\"required\": [ ], \"type\": \"object\"}") Map<Integer, person> employeeRegistry) {};

    @BeforeClass
    public static void setup() {
        registry = new SchemaRegistry();
        SchemaRegistry.set(registry);
    }

    @Test
    public void testPersonSchema() {
        String schema = SchemaRegistry.generateSchema(person.class);
        System.out.println(schema);
    }

}
