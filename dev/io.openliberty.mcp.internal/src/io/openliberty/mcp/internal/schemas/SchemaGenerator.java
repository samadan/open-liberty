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

import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.openliberty.mcp.annotations.Schema;
import io.openliberty.mcp.internal.ToolMetadata;
import io.openliberty.mcp.internal.ToolMetadata.ArgumentMetadata;
import io.openliberty.mcp.internal.schemas.PsuedoSchemaGenerator.ClassPsuedoSchema;
import io.openliberty.mcp.internal.schemas.PsuedoSchemaGenerator.EnumPsuedoSchema;
import io.openliberty.mcp.internal.schemas.PsuedoSchemaGenerator.FieldInfo;
import io.openliberty.mcp.internal.schemas.PsuedoSchemaGenerator.JsonSchema;
import io.openliberty.mcp.internal.schemas.PsuedoSchemaGenerator.JsonSchemaObject;
import io.openliberty.mcp.internal.schemas.PsuedoSchemaGenerator.ListPsuedoSchema;
import io.openliberty.mcp.internal.schemas.PsuedoSchemaGenerator.MapPsuedoSchema;
import io.openliberty.mcp.internal.schemas.PsuedoSchemaGenerator.OptionalPsuedoSchema;
import io.openliberty.mcp.internal.schemas.PsuedoSchemaGenerator.PrimitivePsuedoSchema;
import io.openliberty.mcp.internal.schemas.PsuedoSchemaGenerator.PsuedoSchema;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;

/**
 *
 */
public class SchemaGenerator {
    public static Map<TypeKey, PsuedoSchema> cache = new HashMap<>();

    private static Jsonb jsonb = JsonbBuilder.create();

    public record TypeKey(Type type, SchemaDirection direction) {}

    public static String generateSchema(Class<?> cls, SchemaDirection direction) {
        if (cls.isAnnotationPresent(Schema.class)) {
            Schema schema = cls.getAnnotation(Schema.class);
            try {
                JsonSchema resultObj = jsonb.fromJson(schema.value(), JsonSchema.class);
                return schema.value();
            } catch (JsonbException e) {
                throw new RuntimeException("Schema annotation not valid: " + cls.getName());
            }
        } else {
            TypeKey tmpTKIn = new TypeKey(cls, SchemaDirection.INPUT);
            TypeKey tmpTKOut = new TypeKey(cls, SchemaDirection.OUTPUT);
            if (cls.isRecord()) {
                PsuedoSchema ps = PsuedoSchemaGenerator.generateBaseClassPsuedoSchema(cls, SchemaDirection.INPUT_OUTPUT);
                cache.put(tmpTKIn, ps);
                cache.put(tmpTKOut, ps);
            } else {
                cache.put(tmpTKIn, PsuedoSchemaGenerator.generateBaseClassPsuedoSchema(cls, SchemaDirection.INPUT));
                cache.put(tmpTKOut, PsuedoSchemaGenerator.generateBaseClassPsuedoSchema(cls, SchemaDirection.OUTPUT));
            }

            List<FieldInfo> fields;

            for (FieldInfo fi : ((ClassPsuedoSchema) cache.get(tmpTKIn)).fields()) {
                generatePsuedoSchema(fi.type());
            }
            for (FieldInfo fi : ((ClassPsuedoSchema) cache.get(tmpTKOut)).fields()) {
                generatePsuedoSchema(fi.type());
            }

            HashMap<TypeKey, Boolean> typeFrequency = new HashMap<>();
            HashMap<String, Integer> nameGenerator = new HashMap<>();
            HashMap<TypeKey, String> nameMap = new HashMap<>();
            switch (direction) {
                case INPUT -> calculateClassFrequency(tmpTKIn, typeFrequency, nameGenerator, nameMap);
                case OUTPUT -> calculateClassFrequency(tmpTKOut, typeFrequency, nameGenerator, nameMap);
            }
            HashMap<String, JsonSchema> defs = new HashMap<>();

            JsonSchema result = null;

            switch (direction) {
                case INPUT -> result = cache.get(tmpTKIn).toJsonSchemaObject(direction, nameMap, typeFrequency, defs, true, null);
                case OUTPUT -> result = cache.get(tmpTKOut).toJsonSchemaObject(direction, nameMap, typeFrequency, defs, true, null);
            }
            return jsonb.toJson(result);
        }

    }

    public static String generateToolInputSchema(ToolMetadata tool) {
        // create base schema components
        Map<String, JsonSchema> properties = new HashMap<>();
        List<String> required = new ArrayList<>();
        HashMap<String, JsonSchema> defs = new HashMap<>();
        Parameter[] parameters = tool.method().getJavaMember().getParameters();
        HashMap<TypeKey, String> nameMap = new HashMap<>();
        HashMap<TypeKey, Boolean> typeFrequency = new HashMap<>();
        HashMap<String, Integer> nameGenerator = new HashMap<>();

        // for each parameter
        for (ArgumentMetadata argument : tool.arguments().values()) {
            // - create a pseudo schema
            Parameter type = parameters[argument.index()];
            generatePsuedoSchema(type.getType());
        }

        for (ArgumentMetadata argument : tool.arguments().values()) {
            // - create a pseudo schema
            Parameter type = parameters[argument.index()];

            TypeKey key = new TypeKey(type.getType(), SchemaDirection.INPUT);
            calculateClassFrequency(key, typeFrequency, nameGenerator, nameMap);
        }

        for (var entry : tool.arguments().entrySet()) {
            String argumentName = entry.getKey();
            ArgumentMetadata argument = entry.getValue();
            Parameter type = parameters[argument.index()];

            TypeKey key = new TypeKey(type.getType(), SchemaDirection.INPUT);
            PsuedoSchema ps = cache.get(key);

            JsonSchema parameterSchema = ps.toJsonSchemaObject(SchemaDirection.INPUT, nameMap, typeFrequency, defs, false, argument.description());
            // - add it as a property
            properties.put(argumentName, parameterSchema);
            // - add it as required (if it is)
            if (argument.required()) {
                required.add(argumentName);
            }
        }
        JsonSchemaObject rootSchema = new JsonSchemaObject("object", null, properties, required, defs.isEmpty() ? null : defs, null);
        return jsonb.toJson(rootSchema);
    }

    public static String generateToolOutputSchema(ToolMetadata tool) {
        HashMap<String, JsonSchema> defs = new HashMap<>();
        HashMap<TypeKey, String> nameMap = new HashMap<>();
        HashMap<TypeKey, Boolean> typeFrequency = new HashMap<>();
        HashMap<String, Integer> nameGenerator = new HashMap<>();

        Type returnType = tool.method().getJavaMember().getReturnType();
        generatePsuedoSchema(returnType);
        TypeKey key = new TypeKey(returnType, SchemaDirection.OUTPUT);
        calculateClassFrequency(key, typeFrequency, nameGenerator, nameMap);

        PsuedoSchema ps = cache.get(key);
        JsonSchema outputSchema = ps.toJsonSchemaObject(SchemaDirection.OUTPUT, nameMap, typeFrequency, defs, true, null);
        return jsonb.toJson(outputSchema);
    }

    public static void generatePsuedoSchema(Type type) {
        TypeKey tmpTKIn = new TypeKey(type, SchemaDirection.INPUT);
        TypeKey tmpTKOut = new TypeKey(type, SchemaDirection.OUTPUT);
        if (!cache.containsKey(tmpTKIn) || !cache.containsKey(tmpTKOut)) {
            Type baseType = type;
            if (!isPrimitive(baseType)) {
                if (baseType instanceof Class<?> cls) {
                    if (cls.isEnum()) {
                        EnumPsuedoSchema psuedoSchema = PsuedoSchemaGenerator.generateEnumPsuedoSchema(type);
                        cache.put(tmpTKIn, psuedoSchema);
                        cache.put(tmpTKOut, psuedoSchema);

                    } else if (cls.isArray()) {
                        ListPsuedoSchema psuedoSchema = PsuedoSchemaGenerator.generateArrayPsuedoSchema(type);
                        cache.put(tmpTKIn, psuedoSchema);
                        cache.put(tmpTKOut, psuedoSchema);
                        generatePsuedoSchema(psuedoSchema.itemType());

                    } else if (cls.isRecord()) {
                        ClassPsuedoSchema psuedoSchema = PsuedoSchemaGenerator.generateRecordPsuedoSchema(type);
                        cache.put(tmpTKIn, psuedoSchema);
                        cache.put(tmpTKOut, psuedoSchema);
                        for (FieldInfo fi : psuedoSchema.fields()) {
                            generatePsuedoSchema(fi.type());
                        }

                    } else if (Optional.class.isAssignableFrom(cls)) {
                        OptionalPsuedoSchema psuedoSchema = PsuedoSchemaGenerator.generateRawOptionalPsuedoSchema(type);
                        cache.put(tmpTKIn, psuedoSchema);
                        cache.put(tmpTKOut, psuedoSchema);

                    } else if (Map.class.isAssignableFrom(cls)) {
                        MapPsuedoSchema psuedoSchema = PsuedoSchemaGenerator.generateRawMapPsuedoSchema(type);
                        cache.put(tmpTKIn, psuedoSchema);
                        cache.put(tmpTKOut, psuedoSchema);

                    } else if (Collection.class.isAssignableFrom(cls)) {
                        ListPsuedoSchema psuedoSchema = PsuedoSchemaGenerator.generateRawCollectionPsuedoSchema(type);
                        cache.put(tmpTKIn, psuedoSchema);
                        cache.put(tmpTKOut, psuedoSchema);

                    } else {
                        ClassPsuedoSchema psuedoSchemaIn = PsuedoSchemaGenerator.generateClassPsuedoSchema(type, SchemaDirection.INPUT);
                        ClassPsuedoSchema psuedoSchemaOut = PsuedoSchemaGenerator.generateClassPsuedoSchema(type, SchemaDirection.OUTPUT);
                        cache.put(tmpTKIn, psuedoSchemaIn);
                        cache.put(tmpTKOut, psuedoSchemaOut);

                        for (FieldInfo fi : psuedoSchemaIn.fields()) {
                            generatePsuedoSchema(fi.type());
                        }
                        for (FieldInfo fi : psuedoSchemaOut.fields()) {
                            generatePsuedoSchema(fi.type());
                        }
                    }

                } else if (baseType instanceof ParameterizedType pt) {
                    if (Optional.class.isAssignableFrom((Class<?>) pt.getRawType())) {
                        OptionalPsuedoSchema psuedoSchema = PsuedoSchemaGenerator.generateParameterizedOptionalPsuedoSchema(type);
                        cache.put(tmpTKIn, psuedoSchema);
                        cache.put(tmpTKOut, psuedoSchema);
                        generatePsuedoSchema(psuedoSchema.optionalType());
                    } else if (Map.class.isAssignableFrom((Class<?>) pt.getRawType())) {
                        MapPsuedoSchema psuedoSchema = PsuedoSchemaGenerator.generateParameterizedMapPsuedoSchema(type);
                        cache.put(tmpTKIn, psuedoSchema);
                        cache.put(tmpTKOut, psuedoSchema);
                        generatePsuedoSchema(psuedoSchema.valueType());
                    } else if (Collection.class.isAssignableFrom((Class<?>) pt.getRawType())) {
                        ListPsuedoSchema psuedoSchema = PsuedoSchemaGenerator.generateParameterizedCollectionPsuedoSchema(type);
                        cache.put(tmpTKIn, psuedoSchema);
                        cache.put(tmpTKOut, psuedoSchema);
                        generatePsuedoSchema(psuedoSchema.itemType());

                    }

                }
//                else if (baseType instanceof WildcardType wt) {
//
//                } else if (baseType instanceof GenericArrayType gat) {
//
//                } else if (baseType instanceof TypeVariable<?> td) {
//
//                }
            } else {
                PrimitivePsuedoSchema psuedoSchema = new PrimitivePsuedoSchema(type, type.getClass());
                cache.put(tmpTKIn, psuedoSchema);
                cache.put(tmpTKOut, psuedoSchema);
            }
        }
    }

    public static void calculateClassFrequency(TypeKey typeKey, HashMap<TypeKey, Boolean> typeFrequency, HashMap<String, Integer> nameGenerator, HashMap<TypeKey, String> nameMap) {
        if (!isPrimitive(typeKey.type())) {
            typeFrequency.compute(typeKey, (k, v) -> v == null ? false : true);
            if (typeFrequency.get(typeKey) == false) {
                calculateClassFrequencyTypeBranch(typeKey, typeFrequency, nameGenerator, nameMap);
            }

        }
    }

    public static void calculateClassFrequencyTypeBranch(TypeKey typeKey, HashMap<TypeKey, Boolean> typeFrequency, HashMap<String, Integer> nameGenerator,
                                                         HashMap<TypeKey, String> nameMap) {
        Type baseType = typeKey.type();
        if (isPrimitive(baseType) == true) {
            return;
        }
        if (baseType instanceof Class<?> cls) {
            if (cls.isEnum()) {
                return;

            } else if (cls.isArray()) {
                ListPsuedoSchema ps = (ListPsuedoSchema) cache.get(typeKey);
                calculateClassFrequency(new TypeKey(ps.itemType(), typeKey.direction()), typeFrequency, nameGenerator, nameMap);

            } else if (cls.isRecord() || cls instanceof Class<?>) {
                if (cache.get(typeKey) instanceof ListPsuedoSchema) {
                    System.out.println("erroras List is in place of class");
                }
                ClassPsuedoSchema ps = (ClassPsuedoSchema) cache.get(typeKey);
                String name;
                if (nameGenerator.containsKey(typeKey.type().getClass().getSimpleName())) {
                    if (typeFrequency.get(typeKey) == true) {
                        nameGenerator.compute(cls.getSimpleName(), (k, v) -> v + 1);
                        name = cls.getSimpleName() + nameGenerator.get(cls.getSimpleName());
                    }
                    name = cls.getSimpleName();
                } else {
                    nameGenerator.put(cls.getSimpleName(), 1);
                    name = cls.getSimpleName();
                }
                nameMap.put(typeKey, name);

                for (FieldInfo fi : ps.fields()) {
                    calculateClassFrequency(new TypeKey(fi.type(), typeKey.direction()), typeFrequency, nameGenerator, nameMap);
                }
            }

        } else if (baseType instanceof ParameterizedType pt) {
            if (Optional.class.isAssignableFrom((Class<?>) pt.getRawType())) {
                OptionalPsuedoSchema ps = (OptionalPsuedoSchema) cache.get(typeKey);
                calculateClassFrequency(new TypeKey(ps.optionalType(), typeKey.direction()), typeFrequency, nameGenerator, nameMap);
            } else if (Map.class.isAssignableFrom((Class<?>) pt.getRawType())) {
                MapPsuedoSchema ps = (MapPsuedoSchema) cache.get(typeKey);
                calculateClassFrequency(new TypeKey(ps.valueType(), typeKey.direction()), typeFrequency, nameGenerator, nameMap);
            } else if (Collection.class.isAssignableFrom((Class<?>) pt.getRawType())) {
                ListPsuedoSchema ps = (ListPsuedoSchema) cache.get(typeKey);
                calculateClassFrequency(new TypeKey(ps.itemType(), typeKey.direction()), typeFrequency, nameGenerator, nameMap);

            }

        }
//        else if (baseType instanceof WildcardType wt) {
//
//        } else if (baseType instanceof GenericArrayType gat) {
//
//        } else if (baseType instanceof TypeVariable<?> td) {
//
//        }

    }

    public static boolean isPrimitive(Type type) {
        boolean isJsonNumber = type.equals(long.class) || type.equals(double.class) || type.equals(byte.class) || type.equals(float.class) || type.equals(short.class)
                               || type.equals(Long.class) || type.equals(Double.class) || type.equals(Byte.class) || type.equals(Float.class) || type.equals(Short.class);

        if (isJsonNumber)
            return true;
        else if (type.equals(String.class) || type.equals(Character.class) || type.equals(char.class))
            return true;
        else if (type.equals(int.class) || type.equals(Integer.class))
            return true;
        else if (type.equals(boolean.class) || type.equals(Boolean.class))
            return true;
        else
            return false;

    }

}
