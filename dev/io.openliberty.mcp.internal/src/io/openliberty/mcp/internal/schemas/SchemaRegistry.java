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

import java.util.HashMap;

import io.openliberty.mcp.internal.ToolMetadata;
import jakarta.json.JsonObject;

/**
 *
 */
public class SchemaRegistry {

    private HashMap<SchemaKey, JsonObject> schemaCache = new HashMap<>();

    private SchemaCreationBlueprintRegistry blueprintRegistry = new SchemaCreationBlueprintRegistry();

    /**
     * Gets the JSON schema for a class
     *
     * @param cls .class to generate schema for
     * @param direction whether to get an input or output schema
     * @return the json schema
     */
    public JsonObject getSchema(Class<?> cls, SchemaDirection direction) {
        ClassKey ck = new ClassKey(cls, direction);
        JsonObject schema;
        if (!schemaCache.containsKey(ck)) {
            schema = SchemaGenerator.generateSchema(cls, direction, blueprintRegistry);
            schemaCache.put(ck, schema);

        } else {
            schema = schemaCache.get(ck);

        }
        return schema;

    }

    /**
     * Gets the input JSON schema for a tool
     *
     * @param toolMetadata the tool to get the schema for
     * @return the json schema
     */
    public JsonObject getToolInputSchema(ToolMetadata toolMetadata) {
        ToolKey key = new ToolKey(toolMetadata, SchemaDirection.INPUT);
        return schemaCache.computeIfAbsent(key, k -> SchemaGenerator.generateToolInputSchema(toolMetadata, blueprintRegistry));
    }

    /**
     * Gets the output JSON schema for a tool
     *
     * @param toolMetadata the tool to get the schema for
     * @return the json schema
     */
    public JsonObject getToolOuputSchema(ToolMetadata toolMetadata) {
        ToolKey key = new ToolKey(toolMetadata, SchemaDirection.OUTPUT);
        return schemaCache.computeIfAbsent(key, k -> SchemaGenerator.generateToolOutputSchema(toolMetadata, blueprintRegistry));
    }

    /**
     * Used to access String result from cache regardless if tool or POJO.
     */
    public interface SchemaKey {}

    public record ClassKey(Class<?> cls, SchemaDirection direction) implements SchemaKey {};

    public record ToolKey(ToolMetadata tool, SchemaDirection direction) implements SchemaKey {};

}
