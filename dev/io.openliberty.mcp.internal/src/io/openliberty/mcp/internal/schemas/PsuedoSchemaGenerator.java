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
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.openliberty.mcp.annotations.Schema;
import io.openliberty.mcp.internal.schemas.SchemaGenerator.SchemaGenerationContext;
import io.openliberty.mcp.internal.schemas.TypeUtility.MapTypes;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.json.bind.annotation.JsonbTypeAdapter;

/**
 *
 */
public class PsuedoSchemaGenerator {
    private static Jsonb jsonb = JsonbBuilder.create();

    public static ClassPsuedoSchema generateClassPsuedoSchema(Type type) {
        Class<?> cls = (Class<?>) type;
        List<JsonProperty> properties = JsonProperty.extract(cls);

        return new ClassPsuedoSchema(cls,
                                     getInputFields(properties),
                                     getOutputFields(properties));
    }

    public static EnumPsuedoSchema generateEnumPsuedoSchema(Type type) {
        Class<?> cls = (Class<?>) type;
        List<String> enumValues = getEnumConstants(cls);
        return new EnumPsuedoSchema(cls, enumValues);
    }

    public static ListPsuedoSchema generateRawCollectionPsuedoSchema(Type type) {
        return new ListPsuedoSchema(type, null);
    }

    public static ListPsuedoSchema generateArrayPsuedoSchema(Type type) {
        AnnotatedType elementType = ((AnnotatedArrayType) type).getAnnotatedGenericComponentType();
        return new ListPsuedoSchema(type, elementType.getType());
    }

    public static MapPsuedoSchema generateRawMapPsuedoSchema(Type type) {
        return new MapPsuedoSchema(type, null, null);
    }

    public static OptionalPsuedoSchema generateRawOptionalPsuedoSchema(Type type) {
        return new OptionalPsuedoSchema(type, null);
    }

    public static ListPsuedoSchema generateParameterizedCollectionPsuedoSchema(Type type) {
        Type elementType = TypeUtility.getCollectionType(type);
        return new ListPsuedoSchema(type, elementType);
    }

    public static MapPsuedoSchema generateParameterizedMapPsuedoSchema(Type type) {
        MapTypes mapTypes = TypeUtility.getMapTypes(type);
        Type keyType = mapTypes.key();
        Type valueType = mapTypes.value();
        if (keyType.equals(String.class) || isEnum(keyType)) {
            return new MapPsuedoSchema(type, keyType, valueType);
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

    public static OptionalPsuedoSchema generateParameterizedOptionalPsuedoSchema(Type type) {
        Type elementType = TypeUtility.getOptionalType(type);
        return new OptionalPsuedoSchema(type, elementType);
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

    public static Map<Type, PsuedoSchema> getPsuedoCache() {
        return SchemaGenerator.cache;

    }

    private static String getDefsRef(String name) {
        return "#/$defs/" + name;
    }

    public static class JsonSchemaAdapter implements JsonbAdapter<JsonSchema, JsonObject> {

        /** {@inheritDoc} */
        @Override
        public JsonSchema adaptFromJson(JsonObject arg0) throws Exception {
            System.out.println("adapter reached");
            if (arg0 == null) {
                return null;
            }
            if (arg0.containsKey("items")) {
                // list
                JsonObject defsObj = arg0.getOrDefault("$defs", Json.createObjectBuilder().build()).asJsonObject();
                HashMap<String, JsonSchema> defs = null;
                if (!defsObj.isEmpty()) {
                    defs = new HashMap<>();
                    for (Entry<String, JsonValue> e : defsObj.entrySet()) {
                        defs.put(e.getKey(), adaptFromJson(e.getValue().asJsonObject()));
                    }
                }

                JsonSchema items = adaptFromJson(arg0.getOrDefault("items", JsonValue.NULL).asJsonObject());
                return new JsonSchemaArray(arg0.getString("type", null), arg0.getString("description", null), defs, items);
            } else if (arg0.containsKey("enum")) {
                //enum
                JsonArray enumJsonArray = arg0.getOrDefault("enum", Json.createArrayBuilder().build()).asJsonArray();
                List<String> enumList = new ArrayList<>();
                enumJsonArray.forEach(i -> enumList.add(i.toString()));
                return new JsonSchemaEnum(arg0.getString("type", null), arg0.getString("description", null), enumList);
            } else if (arg0.containsKey("additionalProperties")) {
                //map
                JsonObject defsObj = arg0.getOrDefault("$defs", Json.createObjectBuilder().build()).asJsonObject();
                HashMap<String, JsonSchema> defs = null;
                if (!defsObj.isEmpty()) {
                    defs = new HashMap<>();
                    for (Entry<String, JsonValue> e : defsObj.entrySet()) {
                        defs.put(e.getKey(), adaptFromJson(e.getValue().asJsonObject()));
                    }
                }

                JsonSchema additionslProperties = adaptFromJson(arg0.getOrDefault("additionalProperties", JsonValue.NULL).asJsonObject());
                JsonSchema propertyNames = adaptFromJson(arg0.getOrDefault("propertyNames", JsonValue.NULL).asJsonObject());
                return new JsonSchemaMap(arg0.getString("type", null), arg0.getString("description", null), defs, propertyNames, additionslProperties);
            } else if (arg0.containsKey("properties") || ((arg0.containsKey("$ref")) && (arg0.containsKey("$defs")))) {
                //object
                JsonObject defsObj = arg0.getOrDefault("$defs", Json.createObjectBuilder().build()).asJsonObject();
                HashMap<String, JsonSchema> defs = null;
                if (!defsObj.isEmpty()) {
                    defs = new HashMap<>();
                    for (Entry<String, JsonValue> e : defsObj.entrySet()) {
                        defs.put(e.getKey(), adaptFromJson(e.getValue().asJsonObject()));
                    }
                }

                JsonObject propertiesObj = arg0.getOrDefault("properties", Json.createObjectBuilder().build()).asJsonObject();
                HashMap<String, JsonSchema> properties = null;
                if (!propertiesObj.isEmpty()) {
                    properties = new HashMap<>();
                    for (Entry<String, JsonValue> e : propertiesObj.entrySet()) {
                        properties.put(e.getKey(), adaptFromJson(e.getValue().asJsonObject()));
                    }
                }

                JsonArray requiredJsonArray = arg0.getOrDefault("required", Json.createArrayBuilder().build()).asJsonArray();
                List<String> required = new ArrayList<>();
                requiredJsonArray.forEach(i -> required.add(((JsonString) i).getString()));

                return new JsonSchemaObject(arg0.getString("type", null), arg0.getString("description", null), properties, required, defs, arg0.getString("$ref", null));
            } else {
                //primitive
                return new JsonSchemaPrimitive(arg0.getString("type", null), arg0.getString("description", null), arg0.getString("$ref", null));
            }
        }

        /** {@inheritDoc} */
        @Override
        public JsonObject adaptToJson(JsonSchema arg0) throws Exception {
            System.out.println("serializing  " + arg0.toString());
            String json = jsonb.toJson(arg0);
            return jsonb.fromJson(json, JsonObject.class);
        }

    }

    @JsonbTypeAdapter(JsonSchemaAdapter.class)
    public interface JsonSchema {}

    public record JsonSchemaObject(String type, String description, Map<String, JsonSchema> properties, List<String> required,
                                   @JsonbProperty("$defs") HashMap<String, JsonSchema> defs,
                                   @JsonbProperty("$ref") String ref) implements JsonSchema {}

    public record JsonSchemaArray(String type, String description,
                                  @JsonbProperty("$defs") HashMap<String, JsonSchema> defs, JsonSchema items) implements JsonSchema {}

    public record JsonSchemaMap(String type, String description,
                                @JsonbProperty("$defs") HashMap<String, JsonSchema> defs,
                                JsonSchema propertyNames,
                                JsonSchema additionalProperties) implements JsonSchema {}

    public record JsonSchemaPrimitive(String type, String description, @JsonbProperty("$ref") String ref) implements JsonSchema {}

    public record JsonSchemaPlaceholder() implements JsonSchema {}

    public record JsonSchemaEnum(String type, String description, @JsonbProperty("enum") List<String> enumList) implements JsonSchema {}

    public interface PsuedoSchema {
        public JsonSchema toJsonSchemaObject(SchemaDirection direction, SchemaGenerationContext ctx, boolean baseClass, String description);

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

    public record ClassPsuedoSchema(Class<?> baseType, List<FieldInfo> inputFields, List<FieldInfo> outputFields) implements PsuedoSchema {

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
        public JsonSchema toJsonSchemaObject(SchemaDirection direction, SchemaGenerationContext ctx, boolean baseClass, String referenceDescription) {

            String name = ctx.getName(baseType);

            if (ctx.getDefs().get(baseType) instanceof JsonSchema) {
                return new JsonSchemaPrimitive(null, referenceDescription, getDefsRef(name));
            }

            try {
                Optional<JsonSchema> resultObj = SchemaInfo.read(baseType.getAnnotations()).asJsonSchema();
                if (resultObj.isPresent()) {
                    return resultObj.get();
                }
            } catch (JsonbException e) {
                throw new RuntimeException("Schema annotation not valid: " + name);
            }

            Map<Type, PsuedoSchema> cache = getPsuedoCache();
            Map<String, JsonSchema> properties = new HashMap<>();
            List<String> required = new ArrayList<>();
            List<FieldInfo> fields = direction == SchemaDirection.INPUT ? inputFields() : outputFields();
            for (FieldInfo fi : fields) {
                if (fi.name() == "employees") {
                    System.out.println("");
                }
                SchemaInfo childSchemaAnn = SchemaInfo.read(fi.annotations());
                String fiDescripton = childSchemaAnn.description().orElse(null);

                PsuedoSchema ps = cache.get(fi.type());
                if (!ctx.getDefs().containsKey(baseType)) {
                    ctx.getDefs().put(baseType, new JsonSchemaPlaceholder());
                }

                // Use the schema from the annotation if present, otherwise generate one
                JsonSchema subSchema = childSchemaAnn.schema()
                                                     .map(s -> jsonb.fromJson(s, JsonSchema.class))
                                                     .orElseGet(() -> ps.toJsonSchemaObject(direction, ctx, false, fiDescripton));
                if (fiDescripton != null) {
                    subSchema = SchemaUtils.addDescriptionToJsonSchema(subSchema, fiDescripton);
                }

                if (ctx.getDefs().get(baseType) instanceof JsonSchemaPlaceholder) {
                    ctx.getDefs().remove(baseType);
                }

                properties.put(fi.name(), subSchema);
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

            JsonSchema schema;
            // if base class has defs and base class is not duplicated
            if (baseClass && !ctx.getDefs().isEmpty() && storeInDefs == false) {
                if (name == "address") {
                    System.out.println("address reached");
                }
                HashMap<String, JsonSchema> defs = new HashMap<>();
                ctx.getDefs().forEach((k, v) -> defs.put(ctx.getName(k), v));
                schema = new JsonSchemaObject("object", schemaDescription, properties, required, defs, null);
            } else {
                schema = new JsonSchemaObject("object", schemaDescription, properties, required, null, null);
            }
            // if current class is duplicate
            if (storeInDefs) {
                if (!ctx.getDefs().containsKey(baseType)) {
                    ctx.getDefs().put(baseType, schema);
                }
                HashMap<String, JsonSchema> defs = new HashMap<>();
                ctx.getDefs().forEach((k, v) -> defs.put(ctx.getName(k), v));
                if (baseClass == false) {
                    return new JsonSchemaPrimitive(null, referenceDescription, getDefsRef(name));
                } else {
                    return new JsonSchemaObject(null, referenceDescription, null, null, defs, getDefsRef(name));
                }

            }
            return schema;
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
        public Optional<JsonSchema> asJsonSchema() {
            return schema.map(s -> jsonb.fromJson(s, JsonSchema.class));
        }
    }

    public record ListPsuedoSchema(Type baseType, Type itemType) implements PsuedoSchema {

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
        public JsonSchema toJsonSchemaObject(SchemaDirection direction, SchemaGenerationContext ctx, boolean baseClass, String description) {

//            Schema schemaAnn = (Schema) Arrays.stream(itemType.getAnnotations()).filter(a -> a.annotationType().equals(Schema.class)).findAny().orElse(null);
//            if (schemaAnn != null && schemaAnn.value() != null) {
//                try {
//                    JsonSchema resultObj = jsonb.fromJson(schemaAnn.value(), JsonSchemaObject.class);
//                    return resultObj;
//                } catch (JsonbException e) {
//                    throw new RuntimeException("Schema annotation not valid: " + name);
//                }
//            }

            Map<Type, PsuedoSchema> cache = getPsuedoCache();

            // get description
//            String fiDescripton = schemaAnn != null ? schemaAnn.description() : null;

            PsuedoSchema ps = cache.get(itemType);

            JsonSchema subSchema = ps.toJsonSchemaObject(direction, ctx, false, null);

            JsonSchema schema = new JsonSchemaArray("array", description, null, subSchema);

//            if (typeFrequency.get(baseKey) == Boolean.TRUE) {
//                if (!defsBuilder.containsKey(baseKey)) {
//                    defsBuilder.put(baseKey, schema);
//                }
//
//                HashMap<String, JsonSchema> defs = new HashMap<>();
//                defsBuilder.forEach((k, v) -> defs.put(nameMap.get(k), v));
//
//                if (baseClass == true) {
//                    JsonSchema items = new JsonSchemaPrimitive(null, description, getDefsRef(name));
//                    return new JsonSchemaArray("array", "", defs, items);
//                }
//            }

            if (baseClass && !ctx.getDefs().isEmpty()) {
                HashMap<String, JsonSchema> defs = new HashMap<>();
                ctx.getDefs().forEach((k, v) -> defs.put(ctx.getName(k), v));
                return new JsonSchemaArray("array", description, defs, subSchema);
            }

            return schema;
        }

    }

    public record MapPsuedoSchema(Type baseType, Type keyType, Type valueType) implements PsuedoSchema {

        public MapPsuedoSchema {
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
        public JsonSchema toJsonSchemaObject(SchemaDirection direction, SchemaGenerationContext ctx, boolean baseClass, String description) {

//            Schema schemaAnn = (Schema) Arrays.stream(valueType.getAnnotations()).filter(a -> a.annotationType().equals(Schema.class)).findAny().orElse(null);
//            if (schemaAnn != null && schemaAnn.value() != null) {
//                try {
//                    JsonSchema resultObj = jsonb.fromJson(schemaAnn.value(), JsonSchemaObject.class);
//                    return resultObj;
//                } catch (JsonbException e) {
//                    throw new RuntimeException("Schema annotation not valid: " + name);
//                }
//            }

            Map<Type, PsuedoSchema> cache = getPsuedoCache();

            // get description
//            String fiDescripton = schemaAnn != null ? schemaAnn.description() : null;

            PsuedoSchema ps = cache.get(valueType);
            JsonSchema valueSchema = ps.toJsonSchemaObject(direction, ctx, false, null);
            PsuedoSchema keyPs = cache.get(keyType);
            JsonSchema keySchema = null;
            if (keyPs instanceof EnumPsuedoSchema) {
                keySchema = keyPs.toJsonSchemaObject(direction, ctx, false, null);
            }

            JsonSchemaMap schema = new JsonSchemaMap("object", description, null, keySchema, valueSchema);

//            if (typeFrequency.get(baseKey) == Boolean.TRUE) {
//                if (!defsBuilder.containsKey(baseKey)) {
//                    defsBuilder.put(baseKey, schema);
//                }
//                HashMap<String, JsonSchema> defs = new HashMap<>();
//                defsBuilder.forEach((k, v) -> defs.put(nameMap.get(k), v));
//                if (baseClass == true) {
//                    JsonSchema additionalProperties = new JsonSchemaPrimitive(null, null, getDefsRef(name));
//                    return new JsonSchemaMap("object", description, defs, additionalProperties);
//                }
//            }

            if (baseClass && !ctx.getDefs().isEmpty()) {
                HashMap<String, JsonSchema> defs = new HashMap<>();
                ctx.getDefs().forEach((k, v) -> defs.put(ctx.getName(k), v));
                return new JsonSchemaMap("object", description, defs, keySchema, valueSchema);
            }

            return schema;
        }
    }

    public record EnumPsuedoSchema(Class<?> baseType, List<String> values) implements PsuedoSchema {

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
        public JsonSchema toJsonSchemaObject(SchemaDirection direction, SchemaGenerationContext ctx, boolean baseClass, String description) {

//            Schema schemaAnn = (Schema) Arrays.stream(baseType.getAnnotations()).filter(a -> a.annotationType().equals(Schema.class)).findAny().orElse(null);
//            if (schemaAnn != null && schemaAnn.value() != null) {
//                try {
//                    JsonSchema resultObj = jsonb.fromJson(schemaAnn.value(), JsonSchemaObject.class);
//                    return resultObj;
//                } catch (JsonbException e) {
//                    throw new RuntimeException("Schema annotation not valid: " + name);
//                }
//            }

            // get description
//            String fiDescripton = schemaAnn != null ? schemaAnn.description() : null;

            JsonSchemaEnum schema = new JsonSchemaEnum("string", description, values());

            if (ctx.isMultiUse(baseType)) {
                if (!ctx.getDefs().containsKey(baseType)) {
                    ctx.getDefs().put(baseType, schema);
                }
            }

            return schema;
        }
    }

    public record PrimitivePsuedoSchema(Type baseType, Class<?> primitiveType) implements PsuedoSchema {

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
        public JsonSchema toJsonSchemaObject(SchemaDirection direction, SchemaGenerationContext ctx, boolean baseClass, String description) {

            Type type = baseType;
            boolean isJsonNumber = type.equals(long.class) || type.equals(double.class) || type.equals(byte.class) || type.equals(float.class) || type.equals(short.class)
                                   || type.equals(Long.class) || type.equals(Double.class) || type.equals(Byte.class) || type.equals(Float.class) || type.equals(Short.class);

            if (isJsonNumber)
                return new JsonSchemaPrimitive("number", description, null);
            else if (type.equals(String.class) || type.equals(Character.class) || type.equals(char.class))
                return new JsonSchemaPrimitive("string", description, null);
            else if (type.equals(int.class) || type.equals(Integer.class))
                return new JsonSchemaPrimitive("integer", description, null);
            else if (type.equals(boolean.class) || type.equals(Boolean.class))
                return new JsonSchemaPrimitive("boolean", description, null);
            else {
                return null;
            }
        }
    }

    public record OptionalPsuedoSchema(Type type, Type optionalType) implements PsuedoSchema {

        @Override
        public boolean isRequired() {
            return false;
        }

        @Override
        public Optional<String> defsName() {
            return Optional.empty();
        }

        @Override
        public JsonSchema toJsonSchemaObject(SchemaDirection direction, SchemaGenerationContext ctx, boolean baseClass, String description) {
            PsuedoSchema optionalPs = getPsuedoCache().get(optionalType);
            return optionalPs.toJsonSchemaObject(direction, ctx, baseClass, description);
        }
    }

//    public record WildcardPsuedoSchema(AnnotatedType baseType, AnnotatedType upperType, AnnotatedType lowerType) implements PsuedoSchema {
//
//        /** {@inheritDoc} */
//        @Override
//        public JsonSchemaObject toJsonSchemaObject() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        /** {@inheritDoc} */
//        @Override
//        public JsonSchemaObject toJsonSchemaObject(SchemaDirection direction, String description, String title) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//    }
//
//    public record SchemaPsuedoSchema(AnnotatedType baseType, Schema schema) implements PsuedoSchema {
//
//        /** {@inheritDoc} */
//        @Override
//        public JsonSchemaObject toJsonSchemaObject() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        /** {@inheritDoc} */
//        @Override
//        public JsonSchemaObject toJsonSchemaObject(SchemaDirection direction, String description, String title) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//    }

}
