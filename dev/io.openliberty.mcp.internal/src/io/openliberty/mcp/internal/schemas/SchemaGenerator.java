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
import io.openliberty.mcp.internal.schemas.SchemaCreationContextGenerator.ClassSchemaCreationContext;
import io.openliberty.mcp.internal.schemas.SchemaCreationContextGenerator.FieldInfo;
import io.openliberty.mcp.internal.schemas.SchemaCreationContextGenerator.ListSchemaCreationContext;
import io.openliberty.mcp.internal.schemas.SchemaCreationContextGenerator.MapSchemaCreationContext;
import io.openliberty.mcp.internal.schemas.SchemaCreationContextGenerator.OptionalSchemaCreationContext;
import io.openliberty.mcp.internal.schemas.SchemaCreationContextGenerator.SchemaCreationContext;
import io.openliberty.mcp.internal.schemas.SchemaCreationContextGenerator.SchemaInfo;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;

/**
 *
 */
public class SchemaGenerator {

    private static Jsonb jsonb = JsonbBuilder.create();

    /**
     * <p>If Schema annotation is present then that will be returned if not then a psuedoschema will be generated for all properties that jsonb would serialise for both
     * directions..
     * After the `pseudoschema` is generated the method will check if any classes are duplicated.
     * Using all the context info a JSON schema be generated from psuedoschema.</p>
     *
     * @param cls
     * @param direction
     * @return
     */
    public static String generateSchema(Class<?> cls, SchemaDirection direction) {
        Map<Type, SchemaCreationContext> cache = SchemaCreationContextRegistry.getSchemaCreationContextRegistry().getCache();
        Schema schema = cls.getAnnotation(Schema.class);
        if (schema != null && !schema.value().equals(Schema.UNSET)) {
            try {
                JsonObject resultObj = jsonb.fromJson(schema.value(), JsonObject.class);
                return schema.value();
            } catch (JsonbException e) {
                throw new RuntimeException("Schema annotation not valid: " + cls.getName(), e);
            }

        } else {
            String description = null;
            if (schema != null && !schema.description().equals(Schema.UNSET)) {
                description = schema.description();
            }
            ClassSchemaCreationContext classPs = SchemaCreationContextGenerator.generateClassPsuedoSchema(cls);
            cache.put(cls, SchemaCreationContextGenerator.generateClassPsuedoSchema(cls));

            for (FieldInfo fi : classPs.inputFields()) {
                generateSchemaCreationContext(fi.type());
            }
            for (FieldInfo fi : classPs.outputFields()) {
                generateSchemaCreationContext(fi.type());
            }

            SchemaGenerationContext ctx = new SchemaGenerationContext();
            calculateClassFrequency(cls, direction, ctx);

            JsonObject result = Json.createObjectBuilder().build();

            result = classPs.toJsonSchemaObject(direction, ctx, true, description).build();
            return jsonb.toJson(result);
        }

    }

    public static String generateToolInputSchema(ToolMetadata tool) {
        Map<Type, SchemaCreationContext> cache = SchemaCreationContextRegistry.getSchemaCreationContextRegistry().getCache();
        // create base schema components
        JsonObjectBuilder properties = Json.createObjectBuilder();
        JsonArrayBuilder required = Json.createArrayBuilder();
        Parameter[] parameters = tool.method().getJavaMember().getParameters();
        SchemaGenerationContext ctx = new SchemaGenerationContext();

        // for each parameter
        for (ArgumentMetadata argument : tool.arguments().values()) {
            // - create a pseudo schema
            Parameter type = parameters[argument.index()];
            generateSchemaCreationContext(type.getParameterizedType());
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

            SchemaCreationContext ps = cache.get(type.getParameterizedType());

            JsonObjectBuilder parameterSchemaBuilder = ps.toJsonSchemaObject(SchemaDirection.INPUT, ctx, false, null);
            if (argument.description() != null) {
                parameterSchemaBuilder.add("description", argument.description());
            }
            // - add it as a property
            properties.add(argumentName, parameterSchemaBuilder.build());
            // - add it as required (if it is)
            if (argument.required()) {
                required.add(argumentName);
            }
        }

        JsonObjectBuilder schemaBuilder = Json.createObjectBuilder()
                                              .add("type", "object")
                                              .add("properties", properties.build())
                                              .add("required", required.build());

        if (!ctx.getDefsBuilder().isEmpty()) {
            HashMap<String, JsonObject> defs = new HashMap<>();
            ctx.getDefsBuilder().forEach((k, v) -> defs.put(ctx.getName(k), v));
            schemaBuilder.add("$defs", SchemaCreationContextGenerator.defsToJsonObject(defs));
        }
        return jsonb.toJson(schemaBuilder.build());
    }

    public static String generateToolOutputSchema(ToolMetadata tool) {
        Map<Type, SchemaCreationContext> cache = SchemaCreationContextRegistry.getSchemaCreationContextRegistry().getCache();
        SchemaGenerationContext ctx = new SchemaGenerationContext();

        Type returnType = tool.method().getJavaMember().getGenericReturnType();
        Annotation[] annotations = tool.method().getJavaMember().getAnnotatedReturnType().getAnnotations();
        SchemaInfo returnSchemaAnn = SchemaInfo.read(annotations);
        String description = returnSchemaAnn.description().orElse(null);
        generateSchemaCreationContext(returnType);
        calculateClassFrequency(returnType, SchemaDirection.OUTPUT, ctx);

        SchemaCreationContext ps = cache.get(returnType);
        JsonObjectBuilder outputSchema = ps.toJsonSchemaObject(SchemaDirection.OUTPUT, ctx, true, null);
        if (description != null) {
            outputSchema.add("description", description);
        }
        return jsonb.toJson(outputSchema.build());
    }

    /**
     * generates pseudo schema and then adds it to {@code cache}
     *
     * @param type
     */
    public static SchemaCreationContext generateSchemaCreationContext(Type type) {
        if (!isPrimitive(type)) {
            if (type instanceof Class<?> cls) {
                if (cls.isEnum()) {
                    return SchemaCreationContextGenerator.generateEnumPsuedoSchema(type);

                } else if (cls.isArray()) {
                    return SchemaCreationContextGenerator.generateArrayPsuedoSchema(type);

                } else if (Optional.class.isAssignableFrom(cls)) {
                    return SchemaCreationContextGenerator.generateRawOptionalPsuedoSchema(type);

                } else if (Map.class.isAssignableFrom(cls)) {
                    return SchemaCreationContextGenerator.generateRawMapPsuedoSchema(type);

                } else if (Collection.class.isAssignableFrom(cls)) {
                    return SchemaCreationContextGenerator.generateRawCollectionPsuedoSchema(type);

                } else {
                    ClassSchemaCreationContext schemaCreationContext = SchemaCreationContextGenerator.generateClassPsuedoSchema(type);

//                    for (FieldInfo fi : schemaCreationContext.inputFields()) {
//                        generateSchemaCreationContext(fi.type());
//                    }
//                    for (FieldInfo fi : schemaCreationContext.outputFields()) {
//                        generateSchemaCreationContext(fi.type());
//                    }
                    return schemaCreationContext;
                }

            } else if (type instanceof ParameterizedType pt) {
                if (Optional.class.isAssignableFrom((Class<?>) pt.getRawType())) {
                    return SchemaCreationContextGenerator.generateParameterizedOptionalPsuedoSchema(type);

                } else if (Map.class.isAssignableFrom((Class<?>) pt.getRawType())) {
                    return SchemaCreationContextGenerator.generateParameterizedMapPsuedoSchema(type);

                } else if (Collection.class.isAssignableFrom((Class<?>) pt.getRawType())) {
                    return SchemaCreationContextGenerator.generateParameterizedCollectionPsuedoSchema(type);
                } else {
                    return null;
                }

            } else {
                return null;
                //TODO: WildcardType, GenericArrayType, TypeVariable
                //TODO: Log message and produce simple schema
            }

        } else {
            return SchemaCreationContextRegistry.getSchemaCreationContextRegistry().getCache().get(type);
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
        private HashMap<Type, JsonObject> defsBuilder = new HashMap<>();

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

        public HashMap<Type, JsonObject> getDefsBuilder() {
            return defsBuilder;
        }
    }

    public static void calculateClassFrequency(Type type, SchemaDirection direction, SchemaGenerationContext ctx) {
        Map<Type, SchemaCreationContext> cache = SchemaCreationContextRegistry.getSchemaCreationContextRegistry().getCache();
        SchemaCreationContext scc = cache.computeIfAbsent(type, k -> generateSchemaCreationContext(k));
        boolean previouslySeen = false;
        if (scc.defsName().isPresent()) {
            // We might add this type to defs, so we need to add it to the typeFrequency map
            // If this is the first time we've seen this type, set it to false, if it's not the first time, set it to true
            previouslySeen = ctx.registerSeen(type);

            if (previouslySeen) {
                ctx.reserveName(type, scc.defsName().get());
            }
        }

        if (!previouslySeen) {
            // Process children
            if (scc instanceof ListSchemaCreationContext listPs) {
                calculateClassFrequency(listPs.itemType(), direction, ctx);
            } else if (scc instanceof ClassSchemaCreationContext classPs) {
                List<FieldInfo> fields = direction == SchemaDirection.INPUT ? classPs.inputFields() : classPs.outputFields();
                for (FieldInfo fi : fields) {
                    calculateClassFrequency(fi.type(), direction, ctx);
                }
            } else if (scc instanceof MapSchemaCreationContext mapPs) {
                calculateClassFrequency(mapPs.valueType(), direction, ctx);
                calculateClassFrequency(mapPs.keyType(), direction, ctx);
            } else if (scc instanceof OptionalSchemaCreationContext optionalPs) {
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
