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
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.openliberty.mcp.annotations.Schema;
import io.openliberty.mcp.internal.schemas.SchemaGenerator.SchemaGenerationContext;
import io.openliberty.mcp.internal.schemas.TypeUtility.MapTypes;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;

/**
 *
 */
public class SchemaCreationContextGenerator {
    private static Jsonb jsonb = JsonbBuilder.create();

    public static ClassSchemaCreationContext generateClassPsuedoSchema(Type type) {
        Class<?> cls = (Class<?>) type;
        List<JsonProperty> properties = JsonProperty.extract(cls);

        return new ClassSchemaCreationContext(cls,
                                              getInputFields(properties),
                                              getOutputFields(properties));
    }

    public static EnumSchemaCreationContext generateEnumPsuedoSchema(Type type) {
        Class<?> cls = (Class<?>) type;
        List<String> enumValues = getEnumConstants(cls);
        return new EnumSchemaCreationContext(cls, enumValues);
    }

    public static ListSchemaCreationContext generateRawCollectionPsuedoSchema(Type type) {
        return new ListSchemaCreationContext(type, null);
    }

    public static ListSchemaCreationContext generateArrayPsuedoSchema(Type type) {
        AnnotatedType elementType = ((AnnotatedArrayType) type).getAnnotatedGenericComponentType();
        return new ListSchemaCreationContext(type, elementType.getType());
    }

    public static MapSchemaCreationContext generateRawMapPsuedoSchema(Type type) {
        return new MapSchemaCreationContext(type, null, null);
    }

    public static OptionalSchemaCreationContext generateRawOptionalPsuedoSchema(Type type) {
        return new OptionalSchemaCreationContext(type, null);
    }

    public static ListSchemaCreationContext generateParameterizedCollectionPsuedoSchema(Type type) {
        Type elementType = TypeUtility.getCollectionType(type);
        return new ListSchemaCreationContext(type, elementType);
    }

    public static MapSchemaCreationContext generateParameterizedMapPsuedoSchema(Type type) {
        MapTypes mapTypes = TypeUtility.getMapTypes(type);
        Type keyType = mapTypes.key();
        Type valueType = mapTypes.value();
        if (keyType.equals(String.class) || isEnum(keyType)) {
            return new MapSchemaCreationContext(type, keyType, valueType);
        } else {
            throw new RuntimeException(type + " represents a map which does not have String or Enum keys");
        }
    }

    private static boolean isEnum(Type type) {
        if (type instanceof Class<?> cls) {
            return cls.isEnum();
        } else {
            return false;
        }
    }

    public static OptionalSchemaCreationContext generateParameterizedOptionalPsuedoSchema(Type type) {
        Type elementType = TypeUtility.getOptionalType(type);
        return new OptionalSchemaCreationContext(type, elementType);
    }

    private static List<String> getEnumConstants(Class<?> cls) {
        List<String> result = new ArrayList<>();
        for (Field field : cls.getDeclaredFields()) {
            if (field.isEnumConstant()) {
                String name = field.getAnnotation(JsonbProperty.class) != null ? field.getAnnotation(JsonbProperty.class).value() : field.getName();
                if (field.getAnnotation(JsonbTransient.class) == null)
                    result.add(name);
            }
        }
        return result;
    }

    private static List<FieldInfo> getInputFields(List<JsonProperty> properties) {
        return properties.stream()
                         .filter(p -> p.isInput())
                         .map(p -> new FieldInfo(p.getInputName(), p.getInputType(), p.getInputAnnotations(), SchemaDirection.INPUT))
                         .collect(Collectors.toList());
    }

    private static List<FieldInfo> getOutputFields(List<JsonProperty> properties) {
        return properties.stream()
                         .filter(p -> p.isOutput())
                         .map(p -> new FieldInfo(p.getOutputName(), p.getOutputType(), p.getOutputAnnotations(), SchemaDirection.OUTPUT))
                         .collect(Collectors.toList());
    }

    private static String getDefsRef(String name) {
        return "#/$defs/" + name;
    }

    public interface SchemaCreationContext {
        /**
         * Converts psuedoSchema to jsonSchema recursively based on contextual information.
         *
         * @param direction
         * @param ctx
         * @param baseClass
         * @param description
         * @return
         */
        public JsonObjectBuilder toJsonSchemaObject(SchemaDirection direction, SchemaGenerationContext ctx, boolean baseClass, String description);

        /**
         * Indicates whether instances of this pseudo-schema should be required when included as object properties
         *
         * @return whether schemas created from this pseudo-schema are required
         */
        public boolean isRequired();

        /**
         * Returns the base name that should be used when instances of this pseudo-schema are added to $defs
         *
         * @return the defs name, or an empty optional if this pseudo-schema should not be added to $defs
         */
        public Optional<String> defsName();
    }

    public record ReadWriteDirectionStatus(boolean readable, boolean writeable) {}

    public record FieldInfo(String name, Type type, Annotation[] annotations, SchemaDirection direction) {}

    public static JsonObject defsToJsonObject(Map<String, JsonObject> defs) {
        String json = jsonb.toJson(defs);
        return jsonb.fromJson(json, JsonObject.class);
    }

    public record ClassSchemaCreationContext(Class<?> baseType, List<FieldInfo> inputFields, List<FieldInfo> outputFields) implements SchemaCreationContext {

        @Override
        public boolean isRequired() {
            return true;
        }

        @Override
        public Optional<String> defsName() {
            return Optional.of(baseType.getSimpleName());
        }

        /**
         * <p>If the class has a schema annotation that will be used.
         * If not then that will be validated by converting it to JsonSchema (via jsonbadapter).
         * If the defs builder inside context already contains the basetype then a JsonSchemPrimitive with only a reference will be returned as the type is duplicated.
         * A class json object should contain required list and properities map.
         * Each of the fields in the class is recursively converted to a JsonSchema Object.
         * The final output is dependent on if the class has defs or is a base class thats in defs</p>
         */
        @Override
        public JsonObjectBuilder toJsonSchemaObject(SchemaDirection direction, SchemaGenerationContext ctx, boolean baseClass, String referenceDescription) {

            String name = ctx.getName(baseType);

            if (ctx.getDefsBuilder().get(baseType) instanceof JsonObject) {
                JsonObjectBuilder referenceSchemaBuilder = Json.createObjectBuilder()
                                                               .add("$ref", getDefsRef(name));
                if (referenceDescription != null) {
                    referenceSchemaBuilder.add("description", referenceDescription);
                }
                return referenceSchemaBuilder;

            }

            try {
                Optional<JsonObjectBuilder> resultObj = SchemaInfo.read(baseType.getAnnotations()).asJsonSchema();
                if (resultObj.isPresent()) {
                    return resultObj.get();
                }
            } catch (JsonbException e) {
                throw new RuntimeException("Schema annotation not valid: " + name);
            }

            Map<Type, SchemaCreationContext> cache = SchemaCreationContextRegistry.getSchemaCreationContextRegistry().getCache();
            JsonObjectBuilder properties = Json.createObjectBuilder();
            JsonArrayBuilder required = Json.createArrayBuilder();
            List<FieldInfo> fields = direction == SchemaDirection.INPUT ? inputFields() : outputFields();
            for (FieldInfo fi : fields) {
                SchemaInfo childSchemaAnn = SchemaInfo.read(fi.annotations());
                String fiDescripton = childSchemaAnn.description().orElse(null);

                SchemaCreationContext ps = cache.get(fi.type());
                if (!ctx.getDefsBuilder().containsKey(baseType)) {
                    ctx.getDefsBuilder().put(baseType, Json.createObjectBuilder().build());
                }

                // Use the schema from the annotation if present, otherwise generate one
                JsonObjectBuilder subSchemaObjectBuilder = childSchemaAnn.schema()
                                                                         .map(s -> Json.createObjectBuilder(jsonb.fromJson(s, JsonObject.class)))
                                                                         .orElseGet(() -> ps.toJsonSchemaObject(direction, ctx, false, fiDescripton));
                if (fiDescripton != null) {
                    subSchemaObjectBuilder.add("description", fiDescripton);
                }

                if (ctx.getDefsBuilder().get(baseType).isEmpty()) {
                    ctx.getDefsBuilder().remove(baseType);
                }

                properties.add(fi.name(), subSchemaObjectBuilder.build());

                if (ps.isRequired()) {
                    required.add(fi.name());
                }
            }
            boolean storeInDefs = ctx.isMultiUse(baseType);

            String schemaDescription;
            if (storeInDefs) {
                // We're going to put this schema in defs. We should use the description from the class, if any.
                SchemaInfo baseSchemaAnn = SchemaInfo.read(((Class<?>) baseType()).getAnnotations());
                schemaDescription = baseSchemaAnn.description().orElse(null);

                // If the reference description is the same as the schema description, we can leave it out
                if (Objects.equals(schemaDescription, referenceDescription)) {
                    referenceDescription = null;
                }
            } else {
                // Not putting this schema in defs, use the reference description since this is the only use of this object
                schemaDescription = referenceDescription;
                // Fall back to the schema from the class
                if (schemaDescription == null) {
                    SchemaInfo baseSchemaAnn = SchemaInfo.read(((Class<?>) baseType()).getAnnotations());
                    schemaDescription = baseSchemaAnn.description().orElse(null);
                }
            }

            JsonObjectBuilder schemaBuilder = Json.createObjectBuilder()
                                                  .add("type", "object")
                                                  .add("properties", properties.build())
                                                  .add("required", required.build());
            if (schemaDescription != null) {
                schemaBuilder.add("description", schemaDescription);
            }
            // if base class has defs and base class is not duplicated
            if (baseClass && !ctx.getDefsBuilder().isEmpty() && storeInDefs == false) {
                HashMap<String, JsonObject> defs = new HashMap<>();
                ctx.getDefsBuilder().forEach((k, v) -> defs.put(ctx.getName(k), v));
                schemaBuilder.add("$defs", defsToJsonObject(defs));
            }

            // if current class is duplicate
            if (storeInDefs) {
                // store input schema in defs
                if (!ctx.getDefsBuilder().containsKey(baseType)) {
                    ctx.getDefsBuilder().put(baseType, schemaBuilder.build());
                }

                // reference to defs returned instead of input schema
                schemaBuilder = Json.createObjectBuilder().add("$ref", getDefsRef(name));
                if (referenceDescription != null) {
                    schemaBuilder.add("description", referenceDescription);
                }

                HashMap<String, JsonObject> defs = new HashMap<>();
                ctx.getDefsBuilder().forEach((k, v) -> defs.put(ctx.getName(k), v));
                if (baseClass) {
                    schemaBuilder.add("$defs", defsToJsonObject(defs));
                }
            }
            return schemaBuilder;
        }
    }

    /**
     * The result of reading a {@link Schema} annotation
     */
    public record SchemaInfo(Optional<Schema> annotation, Optional<String> schema, Optional<String> description) {

        private static final SchemaInfo EMPTY = new SchemaInfo(Optional.empty(), Optional.empty(), Optional.empty());

        /**
         * Reads any {@link Schema} annotations in the annotations array
         *
         * @param annotations the annotations array
         * @return the information extracted from any Schema annotations in the array
         */
        public static SchemaInfo read(Annotation[] annotations) {
            return Arrays.stream(annotations)
                         .filter(a -> a.annotationType().equals(Schema.class))
                         .map(Schema.class::cast)
                         .findAny()
                         .map(s -> new SchemaInfo(Optional.of(s), getValue(s), getDescription(s)))
                         .orElse(EMPTY);
        }

        private static Optional<String> getDescription(Schema anno) {
            String description = anno.description();
            return description.equals(Schema.UNSET) ? Optional.empty() : Optional.of(description);
        }

        private static Optional<String> getValue(Schema anno) {
            String value = anno.value();
            return value.equals(Schema.UNSET) ? Optional.empty() : Optional.of(value);
        }

        /**
         * Converts {@link #schema()} to a JsonSchema object, if present
         *
         * @return the JsonSchema
         */
        public Optional<JsonObjectBuilder> asJsonSchema() {
            return schema.map(s -> Json.createObjectBuilder(jsonb.fromJson(s, JsonObject.class)));
        }
    }

    public record ListSchemaCreationContext(Type baseType, Type itemType) implements SchemaCreationContext {

        @Override
        public boolean isRequired() {
            return true;
        }

        @Override
        public Optional<String> defsName() {
            return Optional.empty();
        }

        /** {@inheritDoc} */
        @Override
        public JsonObjectBuilder toJsonSchemaObject(SchemaDirection direction, SchemaGenerationContext ctx, boolean baseClass, String description) {

            Map<Type, SchemaCreationContext> cache = SchemaCreationContextRegistry.getSchemaCreationContextRegistry().getCache();

            SchemaCreationContext ps = cache.get(itemType);

            JsonObjectBuilder itemsSubSchemaBuilder = ps.toJsonSchemaObject(direction, ctx, false, null);

            JsonObjectBuilder schemaBuilder = Json.createObjectBuilder()
                                                  .add("type", "array")
                                                  .add("items", itemsSubSchemaBuilder.build());
            if (description != null) {
                schemaBuilder.add("description", description);
            }

            if (baseClass && !ctx.getDefsBuilder().isEmpty()) {
                HashMap<String, JsonObject> defs = new HashMap<>();
                ctx.getDefsBuilder().forEach((k, v) -> defs.put(ctx.getName(k), v));
                schemaBuilder.add("$defs", defsToJsonObject(defs));
            }

            return schemaBuilder;
        }

    }

    public record MapSchemaCreationContext(Type baseType, Type keyType, Type valueType) implements SchemaCreationContext {

        public MapSchemaCreationContext {
            if (keyType == null) {
                keyType = String.class;
            }
            if (valueType == null) {
                valueType = Object.class;
            }
        }

        @Override
        public boolean isRequired() {
            return true;
        }

        @Override
        public Optional<String> defsName() {
            return Optional.empty();
        }

        /** {@inheritDoc} */
        @Override
        public JsonObjectBuilder toJsonSchemaObject(SchemaDirection direction, SchemaGenerationContext ctx, boolean baseClass, String description) {

            Map<Type, SchemaCreationContext> cache = SchemaCreationContextRegistry.getSchemaCreationContextRegistry().getCache();

            SchemaCreationContext ps = cache.get(valueType);
            JsonObjectBuilder valueSchemaBuilder = ps.toJsonSchemaObject(direction, ctx, false, null);
            SchemaCreationContext keyPs = cache.get(keyType);

            JsonObjectBuilder schemaBuilder = Json.createObjectBuilder()
                                                  .add("type", "object")
                                                  .add("additionalProperties", valueSchemaBuilder.build());

            if (keyPs instanceof EnumSchemaCreationContext) {
                JsonObjectBuilder keySchemaBuilder = keyPs.toJsonSchemaObject(direction, ctx, false, null);
                schemaBuilder.add("propertyNames", keySchemaBuilder.build());
            }
            if (description != null) {
                schemaBuilder.add("description", description);
            }

            if (baseClass && !ctx.getDefsBuilder().isEmpty()) {
                HashMap<String, JsonObject> defs = new HashMap<>();
                ctx.getDefsBuilder().forEach((k, v) -> defs.put(ctx.getName(k), v));
                schemaBuilder.add("$defs", defsToJsonObject(defs));
            }

            return schemaBuilder;
        }
    }

    public record EnumSchemaCreationContext(Class<?> baseType, List<String> values) implements SchemaCreationContext {

        @Override
        public boolean isRequired() {
            return true;
        }

        @Override
        public Optional<String> defsName() {
            return Optional.of(baseType.getSimpleName());
        }

        /** {@inheritDoc} */
        @Override
        public JsonObjectBuilder toJsonSchemaObject(SchemaDirection direction, SchemaGenerationContext ctx, boolean baseClass, String description) {

            JsonArrayBuilder enumValuesJsonArray = Json.createArrayBuilder();
            values.forEach(e -> {
                enumValuesJsonArray.add(e);
            });
            JsonObjectBuilder schemaBuilder = Json.createObjectBuilder().add("type", "string").add("enum", enumValuesJsonArray);
            if (description != null) {
                schemaBuilder.add("description", description);
            }

            if (ctx.isMultiUse(baseType)) {
                if (!ctx.getDefsBuilder().containsKey(baseType)) {
                    ctx.getDefsBuilder().put(baseType, schemaBuilder.build());
                }
                return Json.createObjectBuilder().add("$ref", ctx.getName(baseType));
            }

            return schemaBuilder;
        }
    }

    public record PrimitiveSchemaCreationContext(Type baseType) implements SchemaCreationContext {

        @Override
        public boolean isRequired() {
            return true;
        }

        @Override
        public Optional<String> defsName() {
            return Optional.empty();
        }

        /** {@inheritDoc} */
        @Override
        public JsonObjectBuilder toJsonSchemaObject(SchemaDirection direction, SchemaGenerationContext ctx, boolean baseClass, String description) {

            Type type = baseType;
            boolean isJsonNumber = type.equals(long.class) || type.equals(double.class) || type.equals(byte.class) || type.equals(float.class) || type.equals(short.class)
                                   || type.equals(Long.class) || type.equals(Double.class) || type.equals(Byte.class) || type.equals(Float.class) || type.equals(Short.class);
            JsonObjectBuilder schemaBuilder = Json.createObjectBuilder();
            if (isJsonNumber)
                schemaBuilder.add("type", "number");
            else if (type.equals(String.class) || type.equals(Character.class) || type.equals(char.class))
                schemaBuilder.add("type", "string");
            else if (type.equals(int.class) || type.equals(Integer.class))
                schemaBuilder.add("type", "integer");
            else if (type.equals(boolean.class) || type.equals(Boolean.class))
                return schemaBuilder.add("type", "boolean");
            else {
                return Json.createObjectBuilder();
            }
            if (description != null) {
                schemaBuilder.add("description", description);
            }
            return schemaBuilder;
        }
    }

    public record OptionalSchemaCreationContext(Type type, Type optionalType) implements SchemaCreationContext {

        @Override
        public boolean isRequired() {
            return false;
        }

        @Override
        public Optional<String> defsName() {
            return Optional.empty();
        }

        @Override
        public JsonObjectBuilder toJsonSchemaObject(SchemaDirection direction, SchemaGenerationContext ctx, boolean baseClass, String description) {
            Map<Type, SchemaCreationContext> cache = SchemaCreationContextRegistry.getSchemaCreationContextRegistry().getCache();
            SchemaCreationContext optionalPs = cache.get(optionalType);
            return optionalPs.toJsonSchemaObject(direction, ctx, baseClass, description);
        }
    }

}
