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

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import io.openliberty.mcp.internal.schemas.PsuedoSchemaGenerator.SchemaInfo;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;

/**
 *
 */
public class SchemaGenerator {
    public static Map<Type, PsuedoSchema> cache = new HashMap<>();

    private static Jsonb jsonb = JsonbBuilder.create();

    public static String generateSchema(Class<?> cls, SchemaDirection direction) {
        Schema schema = cls.getAnnotation(Schema.class);
        if (schema != null && !schema.value().equals(Schema.UNSET)) {
            try {
                JsonSchema resultObj = jsonb.fromJson(schema.value(), JsonSchema.class);
                return schema.value();
            } catch (JsonbException e) {
                throw new RuntimeException("Schema annotation not valid: " + cls.getName(), e);
            }

        } else {
            String description = null;
            if (schema != null && !schema.description().equals(Schema.UNSET)) {
                description = schema.description();
            }
            ClassPsuedoSchema classPs = PsuedoSchemaGenerator.generateClassPsuedoSchema(cls);
            cache.put(cls, PsuedoSchemaGenerator.generateClassPsuedoSchema(cls));

            for (FieldInfo fi : classPs.inputFields()) {
                generatePsuedoSchema(fi.type());
            }
            for (FieldInfo fi : classPs.outputFields()) {
                generatePsuedoSchema(fi.type());
            }

            SchemaGenerationContext ctx = new SchemaGenerationContext();
            calculateClassFrequency(cls, direction, ctx);

            JsonSchema result = null;

            result = classPs.toJsonSchemaObject(direction, ctx, true, description);
            return jsonb.toJson(result);
        }

    }

    public static String generateToolInputSchema(ToolMetadata tool) {
        // create base schema components
        Map<String, JsonSchema> properties = new HashMap<>();
        List<String> required = new ArrayList<>();
        Parameter[] parameters = tool.method().getJavaMember().getParameters();
        SchemaGenerationContext ctx = new SchemaGenerationContext();

        // for each parameter
        for (ArgumentMetadata argument : tool.arguments().values()) {
            // - create a pseudo schema
            Parameter type = parameters[argument.index()];
            generatePsuedoSchema(type.getParameterizedType());
        }

        for (ArgumentMetadata argument : tool.arguments().values()) {
            // - create a pseudo schema
            Parameter parameter = parameters[argument.index()];

            calculateClassFrequency(parameter.getParameterizedType(), SchemaDirection.INPUT, ctx);
        }

        for (var entry : tool.arguments().entrySet()) {
            String argumentName = entry.getKey();
            ArgumentMetadata argument = entry.getValue();
            Parameter type = parameters[argument.index()];

            PsuedoSchema ps = cache.get(type.getParameterizedType());

            JsonSchema parameterSchema = ps.toJsonSchemaObject(SchemaDirection.INPUT, ctx, false, null);
            parameterSchema = SchemaUtils.addDescriptionToJsonSchema(parameterSchema, argument.description());
            // - add it as a property
            properties.put(argumentName, parameterSchema);
            // - add it as required (if it is)
            if (argument.required()) {
                required.add(argumentName);
            }
        }
        HashMap<String, JsonSchema> defs = new HashMap<>();
        ctx.getDefs().forEach((k, v) -> defs.put(ctx.getName(k), v));
        JsonSchemaObject rootSchema = new JsonSchemaObject("object", null, properties, required, defs.isEmpty() ? null : defs, null);
        return jsonb.toJson(rootSchema);
    }

    public static String generateToolOutputSchema(ToolMetadata tool) {
        SchemaGenerationContext ctx = new SchemaGenerationContext();

        Type returnType = tool.method().getJavaMember().getGenericReturnType();
        Annotation[] annotations = tool.method().getJavaMember().getAnnotatedReturnType().getAnnotations();
        SchemaInfo returnSchemaAnn = SchemaInfo.read(annotations);
        String description = returnSchemaAnn.description().orElse(null);
        generatePsuedoSchema(returnType);
        calculateClassFrequency(returnType, SchemaDirection.OUTPUT, ctx);

        PsuedoSchema ps = cache.get(returnType);
        JsonSchema outputSchema = ps.toJsonSchemaObject(SchemaDirection.OUTPUT, ctx, true, null);
        outputSchema = SchemaUtils.addDescriptionToJsonSchema(outputSchema, description);
        return jsonb.toJson(outputSchema);
    }

    public static void generatePsuedoSchema(Type type) {
        if (!cache.containsKey(type)) {
            Type baseType = type;
            if (!isPrimitive(baseType)) {
                if (baseType instanceof Class<?> cls) {
                    if (cls.isEnum()) {
                        EnumPsuedoSchema psuedoSchema = PsuedoSchemaGenerator.generateEnumPsuedoSchema(type);
                        cache.put(type, psuedoSchema);

                    } else if (cls.isArray()) {
                        ListPsuedoSchema psuedoSchema = PsuedoSchemaGenerator.generateArrayPsuedoSchema(type);
                        cache.put(type, psuedoSchema);
                        generatePsuedoSchema(psuedoSchema.itemType());

                    } else if (Optional.class.isAssignableFrom(cls)) {
                        OptionalPsuedoSchema psuedoSchema = PsuedoSchemaGenerator.generateRawOptionalPsuedoSchema(type);
                        cache.put(type, psuedoSchema);

                    } else if (Map.class.isAssignableFrom(cls)) {
                        MapPsuedoSchema psuedoSchema = PsuedoSchemaGenerator.generateRawMapPsuedoSchema(type);
                        cache.put(type, psuedoSchema);

                    } else if (Collection.class.isAssignableFrom(cls)) {
                        ListPsuedoSchema psuedoSchema = PsuedoSchemaGenerator.generateRawCollectionPsuedoSchema(type);
                        cache.put(type, psuedoSchema);

                    } else {
                        ClassPsuedoSchema psuedoSchema = PsuedoSchemaGenerator.generateClassPsuedoSchema(type);
                        cache.put(type, psuedoSchema);

                        for (FieldInfo fi : psuedoSchema.inputFields()) {
                            generatePsuedoSchema(fi.type());
                        }
                        for (FieldInfo fi : psuedoSchema.outputFields()) {
                            generatePsuedoSchema(fi.type());
                        }
                    }

                } else if (baseType instanceof ParameterizedType pt) {
                    if (Optional.class.isAssignableFrom((Class<?>) pt.getRawType())) {
                        OptionalPsuedoSchema psuedoSchema = PsuedoSchemaGenerator.generateParameterizedOptionalPsuedoSchema(type);
                        cache.put(type, psuedoSchema);
                        generatePsuedoSchema(psuedoSchema.optionalType());

                    } else if (Map.class.isAssignableFrom((Class<?>) pt.getRawType())) {
                        MapPsuedoSchema psuedoSchema = PsuedoSchemaGenerator.generateParameterizedMapPsuedoSchema(type);
                        cache.put(type, psuedoSchema);
                        generatePsuedoSchema(psuedoSchema.keyType());
                        generatePsuedoSchema(psuedoSchema.valueType());

                    } else if (Collection.class.isAssignableFrom((Class<?>) pt.getRawType())) {
                        ListPsuedoSchema psuedoSchema = PsuedoSchemaGenerator.generateParameterizedCollectionPsuedoSchema(type);
                        cache.put(type, psuedoSchema);
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
                cache.put(type, psuedoSchema);
            }
        }
    }

    public static class SchemaGenerationContext {
        /** Map of type to whether it's been seen more than once */
        private HashMap<Type, Boolean> typeMultiUse = new HashMap<>();
        /** Map of type to name */
        private HashMap<Type, String> nameMap = new HashMap<>();
        /** The values of nameMap */
        private Set<String> namesInUse = new HashSet<>();
        /** Map of types and their corresponding JSON schemas which should be added to defs */
        private HashMap<Type, JsonSchema> defs = new HashMap<>();

        /**
         * Registers a type as having been seen
         *
         * @param type the type
         * @return {@code true} if this method was called for {@code type} before, otherwise {@code false}
         */
        public boolean registerSeen(Type type) {
            // If this is the first time we've seen this type, add it to the map with false,
            // If it's not the first time, set it to true
            return typeMultiUse.compute(type, (k, v) -> v == null ? false : true);
        }

        /**
         * Returns whether a type is used multiple times in the schema.
         *
         * @param type the type
         * @return {@code true} if it was used multiple times, otherwise false
         */
        public boolean isMultiUse(Type type) {
            return typeMultiUse.getOrDefault(type, false);
        }

        /**
         * Reserve a name for a type. The name can be looked up later with {@link #getName(Type)}.
         *
         * @param type the type
         * @param baseName the name to use. A suffix will be added if required to make the name unique.
         */
        public void reserveName(Type type, String baseName) {
            String name = nameMap.get(type);
            if (name == null) {
                int suffix = 1;
                name = baseName;
                while (namesInUse.contains(name)) {
                    suffix++;
                    name = baseName + suffix;
                }
                nameMap.put(type, name);
                namesInUse.add(name);
            }
        }

        /**
         * Get the name for a type.
         * <p>
         * The name must have been reserved earlier using {@link #reserveName(Type, String)}
         *
         * @param type the type
         * @return the name
         */
        public String getName(Type type) {
            return nameMap.get(type);
        }

        /**
         * @return the typeFrequency
         */
        public HashMap<Type, Boolean> getTypeFrequency() {
            return typeMultiUse;
        }

        /**
         * @return the nameMap
         */
        public HashMap<Type, String> getNameMap() {
            return nameMap;
        }

        public HashMap<Type, JsonSchema> getDefs() {
            return defs;
        }
    }

    public static void calculateClassFrequency(Type type, SchemaDirection direction, SchemaGenerationContext ctx) {
        PsuedoSchema ps = cache.get(type);
        boolean previouslySeen = false;
        if (ps.defsName().isPresent()) {
            // We might add this type to defs, so we need to add it to the typeFrequency map
            // If this is the first time we've seen this type, set it to false, if it's not the first time, set it to true
            previouslySeen = ctx.registerSeen(type);

            if (previouslySeen) {
                ctx.reserveName(type, ps.defsName().get());
            }
        }

        if (!previouslySeen) {
            // Process children
            if (ps instanceof ListPsuedoSchema listPs) {
                calculateClassFrequency(listPs.itemType(), direction, ctx);
            } else if (ps instanceof ClassPsuedoSchema classPs) {
                List<FieldInfo> fields = direction == SchemaDirection.INPUT ? classPs.inputFields() : classPs.outputFields();
                for (FieldInfo fi : fields) {
                    calculateClassFrequency(fi.type(), direction, ctx);
                }
            } else if (ps instanceof MapPsuedoSchema mapPs) {
                calculateClassFrequency(mapPs.valueType(), direction, ctx);
                calculateClassFrequency(mapPs.keyType(), direction, ctx);
            } else if (ps instanceof OptionalPsuedoSchema optionalPs) {
                calculateClassFrequency(optionalPs.optionalType(), direction, ctx);
            }
        }
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
