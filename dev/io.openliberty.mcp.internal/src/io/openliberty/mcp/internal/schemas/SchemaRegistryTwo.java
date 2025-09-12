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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;

import io.openliberty.mcp.internal.McpCdiExtension;
import io.openliberty.mcp.internal.ToolMetadata;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 *
 */
public class SchemaRegistryTwo {

    private static HashMap<SchemaKey, String> cacheString = new HashMap<>();

    private static SchemaRegistryTwo staticInstance = null;

    private static Jsonb jsonb = JsonbBuilder.create();

    public static SchemaRegistryTwo getSchemaRegistry() {
        if (staticInstance != null) {
            return staticInstance;
        }
        return CDI.current().select(McpCdiExtension.class).get().getSchemaRegistryTwo();
    }

    /**
     * For unit testing only
     *
     * @param schemRegistry
     */
    public static void set(SchemaRegistryTwo schemaRegistry) {

        staticInstance = schemaRegistry;
    }

    public static String getSchema(Class<?> cls, SchemaDirection direction) {
        ClassKey ck = new ClassKey(cls, direction);
        String schema;
        if (!cacheString.containsKey(ck)) {
            schema = SchemaGenerator.generateSchema(cls, direction);
            cacheString.put(ck, schema);

        } else {
            schema = cacheString.get(ck);

        }
        return schema;

    }

    public static String getToolInputSchema(ToolMetadata toolMetadata) {
        ToolKey key = new ToolKey(toolMetadata, SchemaDirection.INPUT);
        return cacheString.computeIfAbsent(key, k -> SchemaGenerator.generateToolInputSchema(toolMetadata));
    }

    public static String getToolOuputSchema(ToolMetadata toolMetadata) {
        ToolKey key = new ToolKey(toolMetadata, SchemaDirection.OUTPUT);
        return cacheString.computeIfAbsent(key, k -> SchemaGenerator.generateToolOutputSchema(toolMetadata));
    }

    public interface SchemaKey {}

    public record ClassKey(Class<?> cls, SchemaDirection direction) implements SchemaKey {};

    public record ToolKey(ToolMetadata tool, SchemaDirection direction) implements SchemaKey {};

    public record MethodKey(Method method, SchemaDirection direction) implements SchemaKey {};

    public record ParamKey(Parameter param, SchemaDirection direction) implements SchemaKey {};

}
