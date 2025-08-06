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

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.openliberty.mcp.internal.ToolMetadata.ArgumentMetadata;

// TODO "cursor": "optional-cursor-value" (pagination after Tech Exchange)
// TODO build object of objects (we will probably build a schema generator for this) so we could delete the InputSchema object
// TODO method parameter descriptions needs to be defined in the tool annotation

public class ToolDescription {

    private final String name;
    private final String title;
    private final String description;
    private final InputSchemaObject inputSchema;

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public InputSchemaObject getInputSchema() {
        return inputSchema;
    }

    public ToolDescription(ToolMetadata toolMetadata) {

        this.name = toolMetadata.annotation().name();
        this.title = toolMetadata.annotation().title();
        this.description = toolMetadata.annotation().description();

        Map<String, ArgumentMetadata> argumentMap = toolMetadata.arguments();

        InputSchemaPrimitive tempSchemaPrimitive;
        LinkedList<String> tempRequired = new LinkedList<>();
        Map<String, InputSchemaPrimitive> tempProperties = new HashMap<>();

        for (String key : argumentMap.keySet()) {
            Type type = argumentMap.get(key).type();

            if (type.equals(String.class))
                tempSchemaPrimitive = new InputSchemaPrimitive("string", "temp desc");
            else if (type.equals(float.class) || type.equals(double.class))
                tempSchemaPrimitive = new InputSchemaPrimitive("number", "temp desc");
            else if (type.equals(int.class))
                tempSchemaPrimitive = new InputSchemaPrimitive("integer", "temp desc");
            else if (type.equals(boolean.class))
                tempSchemaPrimitive = new InputSchemaPrimitive("boolean", "temp desc");
            else
                tempSchemaPrimitive = new InputSchemaPrimitive(type.getTypeName(), "type conversion for this type is currently not supported");
            //else if (type.equals(Object.class)) {
            // TODO
            //}

            tempProperties.put(key, tempSchemaPrimitive);
            tempRequired.add(key);
        }

        inputSchema = new InputSchemaObject("object", tempProperties, tempRequired);
    }

    public record InputSchema(List<InputSchemaObject> objs, List<InputSchemaPrimitive> primitives) {}

    public record InputSchemaObject(String type, Map<String, InputSchemaPrimitive> properties, List<String> required) {}

    public record InputSchemaPrimitive(String type, String description) {}
}