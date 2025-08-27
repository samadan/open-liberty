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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.openliberty.mcp.annotations.Schema;
import io.openliberty.mcp.annotations.SchemaArg;
import io.openliberty.mcp.internal.McpCdiExtension;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;

public class SchemaRegistry {

    private static SchemaRegistry staticInstance = null;

    private static Jsonb jsonb = JsonbBuilder.create();

    public static SchemaRegistry getSchemaRegistry() {
        if (staticInstance != null) {
            return staticInstance;
        }
        return CDI.current().select(McpCdiExtension.class).get().getSchemaRegistry();
    }

    /**
     * For unit testing only
     *
     * @param schemRegistry
     */
    public static void set(SchemaRegistry schemaRegistry) {
        staticInstance = schemaRegistry;
    }

    private Map<String, String> schemas = new HashMap<>();

    public static String generateSchema(Class<?> cls) {
        HashSet<Class<?>> seen = new HashSet<>();
        seen.add(cls);
        JsonSchemaObject schemaObj = generateSchemObject(cls, seen);
        return jsonb.toJson(schemaObj);
    }

    private static JsonSchemaObject generateSchemObject(Class<?> cls, Set<Class<?>> seen) {
        JsonObjectBuilder jsonObject = Json.createObjectBuilder();
        if (cls.isAnnotationPresent(Schema.class)) {
            Schema schema = cls.getAnnotation(Schema.class);
            try {
                return jsonb.fromJson(schema.value(), JsonSchemaObject.class);
            } catch (JsonbException e) {
                throw new RuntimeException("Schema annotation not valid: " + cls.getName());
            }

        } else {

            Map<String, Object> schemaMap = new HashMap<>();
            LinkedList<String> requiredParameterList = new LinkedList<>();
            Field[] fields = cls.getDeclaredFields();
            for (Field field : fields) {
                if (field.getAnnotation(JsonbTransient.class) == null) {
                    String argumentName = field.getAnnotation(JsonbProperty.class) != null ? field.getAnnotation(JsonbProperty.class).value() : field.getName();
                    schemaMap.put(argumentName, buildSchema(field, seen));
                    requiredParameterList.add(argumentName);
                }

            }

            return new JsonSchemaObject("object", schemaMap, requiredParameterList);

        }

    }

    private static Object buildSchema(Field field, Set<Class<?>> seen) {
        Object tempSchema;
        if (!field.isAnnotationPresent(SchemaArg.class)) {
            Type type = field.getGenericType();
            String description = null;

            boolean isJsonNumber = type.equals(long.class) || type.equals(double.class) || type.equals(byte.class) || type.equals(float.class) || type.equals(short.class)
                                   || type.equals(Long.class) || type.equals(Double.class) || type.equals(Byte.class) || type.equals(Float.class) || type.equals(Short.class);

            if (isJsonNumber)
                tempSchema = new JsonSchemaPrimitive("number", description);
            else if (type.equals(String.class) || type.equals(Character.class) || type.equals(char.class))
                tempSchema = new JsonSchemaPrimitive("string", description);
            else if (type.equals(int.class) || type.equals(Integer.class))
                tempSchema = new JsonSchemaPrimitive("integer", description);
            else if (type.equals(boolean.class) || type.equals(Boolean.class))
                tempSchema = new JsonSchemaPrimitive("boolean", description);
            else if (type.equals(List.class) || type.equals(Set.class) || type.equals(Array.class)) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Type subtype = parameterizedType.getActualTypeArguments()[0];
                tempSchema = new JsonSchemaArray("array", description, generateSchemObject(
                                                                                           (Class<?>) ((ParameterizedType) subtype).getRawType(), seen));
            } else if (type.equals(Array.class)) {
                tempSchema = new JsonSchemaArray("array", description, generateSchemObject(field.getType().getComponentType(), seen));
            } else if (type.equals(Map.class)) {
                tempSchema = new JsonSchemaPrimitive("object", description);
            } else {
                if (!seen.contains(type)) {
                    seen.add((Class<?>) type);
                    tempSchema = generateSchemObject((Class<?>) type, seen);
                } else {
                    tempSchema = new JsonSchemaPrimitive("object<" + field.getType().getTypeName() + ">", description);
                }
            }

        } else {
            SchemaArg schema = field.getAnnotation(SchemaArg.class);
            try {
                return jsonb.fromJson(schema.value(), JsonSchemaObject.class);
            } catch (JsonbException e) {
                throw new RuntimeException("SchemaArg annotation not valid for feild: " + field.getType().getTypeName());
            }
        }
        return tempSchema;

    }

    public record JsonSchema(List<JsonSchemaObject> objs, List<JsonSchemaPrimitive> primitives) {}

    public record JsonSchemaObject(String type, Map<String, Object> properties, List<String> required) {}

    public record JsonSchemaArray(String type, String description, Object items) {}

    public record JsonSchemaPrimitive(String type, String description) {}

}
