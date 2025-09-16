/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.schemas;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.openliberty.mcp.internal.McpCdiExtension;
import io.openliberty.mcp.internal.schemas.SchemaCreationContextGenerator.PrimitiveSchemaCreationContext;
import io.openliberty.mcp.internal.schemas.SchemaCreationContextGenerator.SchemaCreationContext;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 *
 */
public class SchemaCreationContextRegistry {
    private Map<Type, SchemaCreationContext> cache = new HashMap<>();

    public SchemaCreationContextRegistry() {
        List<Class<?>> classes = List.of(long.class, byte.class, double.class, float.class, short.class,
                                         Long.class, Byte.class, Double.class, Float.class, Short.class,
                                         String.class, char.class, Character.class,
                                         int.class, Integer.class,
                                         boolean.class, Boolean.class);

        classes.forEach(type -> cache.put(type, new PrimitiveSchemaCreationContext(type)));
    }

    private static SchemaCreationContextRegistry staticInstance = null;

    private static Jsonb jsonb = JsonbBuilder.create();

    public static SchemaCreationContextRegistry getSchemaCreationContextRegistry() {
        if (staticInstance != null) {
            return staticInstance;
        }
        return CDI.current().select(McpCdiExtension.class).get().getSchemaCreationContextRegistry();
    }

    /**
     * For unit testing only
     *
     * @param schemRegistry
     */
    public static void set(SchemaCreationContextRegistry schemaCreationContextRegistry) {

        staticInstance = schemaCreationContextRegistry;
    }

    public Map<Type, SchemaCreationContext> getCache() {
        return cache;
    }

}
