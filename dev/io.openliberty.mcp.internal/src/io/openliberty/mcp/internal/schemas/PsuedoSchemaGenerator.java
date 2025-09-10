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
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import io.openliberty.mcp.annotations.Schema;
import io.openliberty.mcp.internal.schemas.SchemaGenerator.TypeKey;
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

    public static ClassPsuedoSchema generateClassPsuedoSchema(Type type, SchemaDirection direction) {
        List<FieldInfo> fields = null;
        Class<?> cls = (Class<?>) type;
        switch (direction) {
            case INPUT -> fields = getInputFields(cls);
            case OUTPUT -> fields = getOutputFields(cls);
        }

        return new ClassPsuedoSchema(cls, fields, cls.getAnnotations() != null ? cls.getAnnotations() : new Annotation[] {});

    }

    public static ClassPsuedoSchema generateBaseClassPsuedoSchema(Class<?> cls, SchemaDirection direction) {
        List<FieldInfo> fields = null;
        switch (direction) {
            case INPUT -> fields = getInputFields(cls);
            case OUTPUT -> fields = getOutputFields(cls);
            case INPUT_OUTPUT -> fields = getExplicitDefinedFields(cls);
        }
        return new ClassPsuedoSchema(cls, fields, cls.getAnnotations());
    }

    public static ClassPsuedoSchema generateRecordPsuedoSchema(Type type) {
        Class<?> cls = (Class<?>) type;
        List<FieldInfo> fields = getExplicitDefinedFields(cls);
        return new ClassPsuedoSchema(cls, fields, cls.getAnnotations() != null ? cls.getAnnotations() : new Annotation[] {});
    }

    public static EnumPsuedoSchema generateEnumPsuedoSchema(Type type) {
        Class<?> cls = (Class<?>) type;
        List<String> enumValues = getEnumConstants(cls);
        return new EnumPsuedoSchema(type, enumValues);
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
        AnnotatedType elementType = ((AnnotatedParameterizedType) type).getAnnotatedActualTypeArguments()[0];
        return new OptionalPsuedoSchema(type, elementType.getType());
    }

    private static List<String> getEnumConstants(Class<?> cls) {
        List<String> result = new ArrayList<>();
        for (Field field : cls.getDeclaredFields()) {
            if (field.isEnumConstant()) {
                String name = field.getAnnotation(JsonbProperty.class) != null ? field.getAnnotation(JsonbProperty.class).value() : field.getName();
                if (field.getAnnotation(JsonbTransient.class) != null)
                    result.add(name);
            }
        }
        return result;
    }

    private static List<FieldInfo> getInputFields(Class<?> cls) {
        List<FieldInfo> result = new ArrayList<>();
        for (Method method : cls.getDeclaredMethods()) {
            if ((method.getReturnType() == void.class) && (method.getName().startsWith("set")) && method.getParameterCount() == 1) {
                String name = method.getAnnotation(JsonbProperty.class) != null ? method.getAnnotation(JsonbProperty.class).value() : method.getName().substring(3).toLowerCase();
                FieldInfo fi = new FieldInfo(name, method.getGenericParameterTypes()[0],
                                             method.getDeclaredAnnotations() != null ? method.getDeclaredAnnotations() : new Annotation[] {}, SchemaDirection.INPUT);
                if (method.getAnnotation(JsonbTransient.class) == null)
                    result.add(fi);
            }
        }
        result.addAll(getExplicitDefinedFields(cls));
        return result;
    }

    private static List<FieldInfo> getOutputFields(Class<?> cls) {
        List<FieldInfo> result = new ArrayList<>();
        for (Method method : cls.getDeclaredMethods()) {
            if ((method.getReturnType() != void.class) && method.getParameterCount() == 0 && (method.getName().startsWith("get"))) {
                String name = method.getAnnotation(JsonbProperty.class) != null ? method.getAnnotation(JsonbProperty.class).value() : method.getName().substring(3).toLowerCase();
                FieldInfo fi = new FieldInfo(name, method.getGenericReturnType(),
                                             method.getDeclaredAnnotations() != null ? method.getDeclaredAnnotations() : new Annotation[] {}, SchemaDirection.OUTPUT);
                if (method.getAnnotation(JsonbTransient.class) == null)
                    result.add(fi);
            } else if ((method.getReturnType() == boolean.class) && method.getParameterCount() == 0 && (method.getName().startsWith("is"))) {
                String name = method.getAnnotation(JsonbProperty.class) != null ? method.getAnnotation(JsonbProperty.class).value() : method.getName().substring(2).toLowerCase();
                FieldInfo fi = new FieldInfo(name, method.getGenericReturnType(),
                                             method.getDeclaredAnnotations() != null ? method.getDeclaredAnnotations() : new Annotation[] {}, SchemaDirection.OUTPUT);
                if (method.getAnnotation(JsonbTransient.class) == null)
                    result.add(fi);
            }
        }
        result.addAll(getExplicitDefinedFields(cls));
        return result;
    }

    private static List<FieldInfo> getExplicitDefinedFields(Class<?> cls) {
        List<FieldInfo> result = new ArrayList<>();
        for (Field field : cls.getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers()) || cls.isRecord()) {
                String name = field.getAnnotation(JsonbProperty.class) != null ? field.getAnnotation(JsonbProperty.class).value() : field.getName();
                FieldInfo fi = new FieldInfo(name, field.getGenericType(), field.getDeclaredAnnotations() != null ? field.getDeclaredAnnotations() : new Annotation[] {},
                                             SchemaDirection.INPUT_OUTPUT);
                if (field.getAnnotation(JsonbTransient.class) == null)
                    result.add(fi);
            } else if (Modifier.isPrivate(field.getModifiers()) && field.getAnnotation(JsonbProperty.class) != null) {
                String name = field.getAnnotation(JsonbProperty.class).value();
                FieldInfo fi = new FieldInfo(name, field.getGenericType(), field.getDeclaredAnnotations() != null ? field.getDeclaredAnnotations() : new Annotation[] {},
                                             SchemaDirection.INPUT_OUTPUT);
                if (field.getAnnotation(JsonbTransient.class) == null)
                    result.add(fi);
            }
        }
        return result;
    }

    public static Map<TypeKey, PsuedoSchema> getPsuedoCache() {
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
                return new JsonSchemaMap(arg0.getString("type", null), arg0.getString("description", null), defs, additionslProperties);
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
                                @JsonbProperty("$defs") HashMap<String, JsonSchema> defs, JsonSchema additionalProperties) implements JsonSchema {}

    public record JsonSchemaPrimitive(String type, String description, @JsonbProperty("$ref") String ref) implements JsonSchema {}

    public record JsonSchemaPlaceholder() implements JsonSchema {}

    public record JsonSchemaEnum(String type, String description, @JsonbProperty("enum") List<String> enumList) implements JsonSchema {}

    public interface PsuedoSchema {
        public JsonSchema toJsonSchemaObject(SchemaDirection direction, HashMap<TypeKey, String> nameMap, HashMap<TypeKey, Boolean> typeFrequency,
                                             HashMap<TypeKey, JsonSchema> defsBuilder, boolean baseClass, String description);
    }

    public record ReadWriteDirectionStatus(boolean readable, boolean writeable) {}

    public record FieldInfo(String name, Type type, Annotation[] annotations, SchemaDirection direction) {}

    public record ClassPsuedoSchema(Type baseType, List<FieldInfo> fields, Annotation[] annotations) implements PsuedoSchema {

        /** {@inheritDoc} */
        @Override
        public JsonSchema toJsonSchemaObject(SchemaDirection direction, HashMap<TypeKey, String> nameMap, HashMap<TypeKey, Boolean> typeFrequency,
                                             HashMap<TypeKey, JsonSchema> defsBuilder, boolean baseClass, String description) {

            TypeKey baseKey = new TypeKey(baseType, direction);
            String name = nameMap.get(baseKey);
            if (name == "company") {
                System.out.println("");
            }
            if (defsBuilder.containsKey(baseKey) && defsBuilder.get(baseKey) instanceof JsonSchema) {
                return new JsonSchemaPrimitive(null, description, getDefsRef(name));
            }

            SchemaInfo schemaAnn = SchemaInfo.parse(annotations());
            if (schemaAnn.schema().isPresent()) {
                try {
                    JsonSchema resultObj = jsonb.fromJson(schemaAnn.schema.get(), JsonSchema.class);
                    return resultObj;
                } catch (JsonbException e) {
                    throw new RuntimeException("Schema annotation not valid: " + name);
                }

            }

            Map<TypeKey, PsuedoSchema> cache = getPsuedoCache();
            Map<String, JsonSchema> properties = new HashMap<>();
            List<String> required = new ArrayList<>();
            for (FieldInfo fi : fields()) {
                SchemaInfo childSchemaAnn = SchemaInfo.parse(fi.annotations());
                String fiDescripton = childSchemaAnn.description().orElse(null);

                TypeKey tmpTK = new TypeKey(fi.type(), direction);
                PsuedoSchema ps = cache.get(tmpTK);
                if (!defsBuilder.containsKey(baseKey)) {
                    defsBuilder.put(baseKey, new JsonSchemaPlaceholder());
                }

                // Use the schema from the annotation if present, otherwise generate one
                JsonSchema subSchema = childSchemaAnn.schema()
                                                     .map(s -> jsonb.fromJson(s, JsonSchema.class))
                                                     .orElseGet(() -> ps.toJsonSchemaObject(direction, nameMap, typeFrequency, defsBuilder, false, fiDescripton));
                if (fiDescripton != null) {
                    if (subSchema instanceof JsonSchemaObject jso) {
                        subSchema = new JsonSchemaObject(jso.type(), jso.description() != null ? fiDescripton : null, jso.properties(), jso.required(), jso.defs(), jso.ref());
                    } else if (subSchema instanceof JsonSchemaArray jsa) {
                        subSchema = new JsonSchemaArray(jsa.type(), jsa.description() != null ? fiDescripton : null, jsa.defs(), jsa.items());
                    } else if (subSchema instanceof JsonSchemaMap jsm) {
                        subSchema = new JsonSchemaMap(jsm.type(), jsm.description() != null ? fiDescripton : null, jsm.defs(), jsm.additionalProperties());
                    } else if (subSchema instanceof JsonSchemaEnum jse) {
                        subSchema = new JsonSchemaEnum(jse.type(), jse.description() != null ? fiDescripton : null, jse.enumList());
                    } else if (subSchema instanceof JsonSchemaPrimitive jsp) {
                        subSchema = new JsonSchemaPrimitive(jsp.type(), jsp.description() != null ? fiDescripton : null, jsp.ref());
                    }
                }
                if (defsBuilder.containsKey(baseKey) && defsBuilder.get(baseKey) instanceof JsonSchemaPlaceholder) {
                    defsBuilder.remove(baseKey);
                }

                properties.put(fi.name(), subSchema);
                if (fi.type() instanceof ParameterizedType && Optional.class.equals(fi.type())) {
                    continue;
                } else {
                    required.add(fi.name());
                }
            }
            boolean storeInDefs = typeFrequency.get(baseKey);
            String schemaDescription = storeInDefs ? null : description;
            schemaDescription = (baseClass && typeFrequency.get(baseKey)) ? description : null;

            JsonSchema schema;

            if (baseClass && !defsBuilder.isEmpty() && typeFrequency.get(baseKey) == false) {
                if (name == "address") {
                    System.out.println("address reached");
                }
                HashMap<String, JsonSchema> defs = new HashMap<>();
                defsBuilder.forEach((k, v) -> defs.put(nameMap.get(k), v));
                schema = new JsonSchemaObject("object", schemaDescription, properties, required, defs, null);
            } else {
                schema = new JsonSchemaObject("object", schemaDescription, properties, required, null, null);
            }

            if (typeFrequency.get(baseKey) == true) {
                if (!defsBuilder.containsKey(baseKey)) {
                    defsBuilder.put(baseKey, schema);
                }
                HashMap<String, JsonSchema> defs = new HashMap<>();
                defsBuilder.forEach((k, v) -> defs.put(nameMap.get(k), v));
                if (baseClass == false) {
                    return new JsonSchemaPrimitive(null, schemaDescription, getDefsRef(name));
                } else {
                    return new JsonSchemaObject(null, null, null, null, defs, getDefsRef(name));
                }

            }
            return schema;
        }
    }

    public record SchemaInfo(Optional<String> schema, Optional<String> description) {

        /**
         * Parses any {@link Schema} annotations in the annotations array
         *
         * @param annotations the annotations array
         * @return the information extracted from any Schema annotations in the array
         */
        public static SchemaInfo parse(Annotation[] annotations) {
            return Arrays.stream(annotations)
                         .filter(a -> a.annotationType().equals(Schema.class))
                         .map(Schema.class::cast)
                         .findAny()
                         .map(s -> new SchemaInfo(getValue(s), getDescription(s)))
                         .orElseGet(() -> new SchemaInfo(Optional.empty(), Optional.empty()));
        }

        private static Optional<String> getDescription(Schema anno) {
            String description = anno.description();
            return description.equals(Schema.UNSET) ? Optional.empty() : Optional.of(description);
        }

        private static Optional<String> getValue(Schema anno) {
            String value = anno.value();
            return value.equals(Schema.UNSET) ? Optional.empty() : Optional.of(value);
        }
    }

    public record ListPsuedoSchema(Type baseType, Type itemType) implements PsuedoSchema {

        /** {@inheritDoc} */
        @Override
        public JsonSchema toJsonSchemaObject(SchemaDirection direction, HashMap<TypeKey, String> nameMap, HashMap<TypeKey, Boolean> typeFrequency,
                                             HashMap<TypeKey, JsonSchema> defsBuilder, boolean baseClass, String description) {

            TypeKey baseKey = new TypeKey(baseType, direction);

//            Schema schemaAnn = (Schema) Arrays.stream(itemType.getAnnotations()).filter(a -> a.annotationType().equals(Schema.class)).findAny().orElse(null);
//            if (schemaAnn != null && schemaAnn.value() != null) {
//                try {
//                    JsonSchema resultObj = jsonb.fromJson(schemaAnn.value(), JsonSchemaObject.class);
//                    return resultObj;
//                } catch (JsonbException e) {
//                    throw new RuntimeException("Schema annotation not valid: " + name);
//                }
//            }

            Map<TypeKey, PsuedoSchema> cache = getPsuedoCache();

            // get description
//            String fiDescripton = schemaAnn != null ? schemaAnn.description() : null;

            TypeKey tmpTK = new TypeKey(itemType, direction);
            PsuedoSchema ps = cache.get(tmpTK);
            JsonSchema subSchema = ps.toJsonSchemaObject(direction, nameMap, typeFrequency, defsBuilder, false, null);

            JsonSchema schema = new JsonSchemaArray("array", description, null, subSchema);

//            if (typeFrequency.get(baseKey) == true) {
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
            TypeKey itemKey = new TypeKey(itemType, direction);
            if (typeFrequency.get(itemKey) == true && baseClass == true) {
                HashMap<String, JsonSchema> defs = new HashMap<>();
                defsBuilder.forEach((k, v) -> defs.put(nameMap.get(k), v));
                String name = nameMap.get(itemKey);
                JsonSchema items = new JsonSchemaPrimitive(null, description, getDefsRef(name));
                return new JsonSchemaArray("array", description, defs, items);
            }

            return schema;
        }

    }

    public record MapPsuedoSchema(Type baseType, Type keyType, Type valueType) implements PsuedoSchema {

        /** {@inheritDoc} */
        @Override
        public JsonSchema toJsonSchemaObject(SchemaDirection direction, HashMap<TypeKey, String> nameMap, HashMap<TypeKey, Boolean> typeFrequency,
                                             HashMap<TypeKey, JsonSchema> defsBuilder, boolean baseClass, String description) {

            TypeKey baseKey = new TypeKey(baseType, direction);

//            Schema schemaAnn = (Schema) Arrays.stream(valueType.getAnnotations()).filter(a -> a.annotationType().equals(Schema.class)).findAny().orElse(null);
//            if (schemaAnn != null && schemaAnn.value() != null) {
//                try {
//                    JsonSchema resultObj = jsonb.fromJson(schemaAnn.value(), JsonSchemaObject.class);
//                    return resultObj;
//                } catch (JsonbException e) {
//                    throw new RuntimeException("Schema annotation not valid: " + name);
//                }
//            }

            Map<TypeKey, PsuedoSchema> cache = getPsuedoCache();

            // get description
//            String fiDescripton = schemaAnn != null ? schemaAnn.description() : null;

            TypeKey tmpTK = new TypeKey(valueType, direction);
            PsuedoSchema ps = cache.get(tmpTK);
            JsonSchema valueSchema = ps.toJsonSchemaObject(direction, nameMap, typeFrequency, defsBuilder, false, null);

            JsonSchemaMap schema = new JsonSchemaMap("object", description, null, valueSchema);

//            if (typeFrequency.get(baseKey) == true) {
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
            TypeKey valueKey = new TypeKey(valueType, direction);
            if (typeFrequency.get(valueKey) == true && baseClass == true) {
                HashMap<String, JsonSchema> defs = new HashMap<>();
                defsBuilder.forEach((k, v) -> defs.put(nameMap.get(k), v));

                String name = nameMap.get(valueKey);
                JsonSchema additionalProperties = new JsonSchemaPrimitive(null, null, getDefsRef(name));
                return new JsonSchemaMap("object", description, defs, additionalProperties);
            }

            return schema;
        }
    }

    public record EnumPsuedoSchema(Type baseType, List<String> values) implements PsuedoSchema {

        /** {@inheritDoc} */
        @Override
        public JsonSchema toJsonSchemaObject(SchemaDirection direction, HashMap<TypeKey, String> nameMap, HashMap<TypeKey, Boolean> typeFrequency,
                                             HashMap<TypeKey, JsonSchema> defsBuilder, boolean baseClass, String description) {

            TypeKey baseKey = new TypeKey(baseType, direction);
            String name = nameMap.get(baseKey);

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

            if (typeFrequency.get(baseKey) == true) {
                if (!defsBuilder.containsKey(baseKey)) {
                    defsBuilder.put(baseKey, schema);
                }
            }

            return schema;
        }
    }

    public record PrimitivePsuedoSchema(Type baseType, Class<?> primitiveType) implements PsuedoSchema {
        /** {@inheritDoc} */
        @Override
        public JsonSchema toJsonSchemaObject(SchemaDirection direction, HashMap<TypeKey, String> nameMap, HashMap<TypeKey, Boolean> typeFrequency,
                                             HashMap<TypeKey, JsonSchema> defs, boolean baseClass, String description) {

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

    public record OptionalPsuedoSchema(Type baseType, Type optionalType) implements PsuedoSchema {
        /** {@inheritDoc} */
        @Override
        public JsonSchema toJsonSchemaObject(SchemaDirection direction, HashMap<TypeKey, String> nameMap, HashMap<TypeKey, Boolean> typeFrequency,
                                             HashMap<TypeKey, JsonSchema> defsBuilder, boolean baseClass, String description) {

            TypeKey baseKey = new TypeKey(baseType, direction);

//            Schema schemaAnn = (Schema) Arrays.stream(optionalType.getAnnotations()).filter(a -> a.annotationType().equals(Schema.class)).findAny().orElse(null);
//            if (schemaAnn != null && schemaAnn.value() != null) {
//                try {
//                    JsonSchema resultObj = jsonb.fromJson(schemaAnn.value(), JsonSchemaObject.class);
//                    return resultObj;
//                } catch (JsonbException e) {
//                    throw new RuntimeException("Schema annotation not valid: " + name);
//                }
//            }

            Map<TypeKey, PsuedoSchema> cache = getPsuedoCache();

            // get description
//            String fiDescripton = schemaAnn != null ? schemaAnn.description() : null;

            TypeKey tmpTK = new TypeKey(optionalType, direction);
            PsuedoSchema ps = cache.get(tmpTK);
            JsonSchema subSchema = ps.toJsonSchemaObject(direction, nameMap, typeFrequency, defsBuilder, false, null);

            if (typeFrequency.get(baseKey) == true && baseClass == true) {
//                if (!defsBuilder.containsKey(baseKey)) {
//                    defsBuilder.put(baseKey, subSchema);
//                }
                HashMap<String, JsonSchema> defs = new HashMap<>();
                defsBuilder.forEach((k, v) -> defs.put(nameMap.get(k), v));

                if (subSchema instanceof JsonSchemaObject jso) {
                    Type t = ((ClassPsuedoSchema) ps).baseType();
                    TypeKey optionalItemKey = new TypeKey(t, direction);
                    String name = nameMap.get(optionalItemKey);
                    return new JsonSchemaObject(null, null, null, null, defs, getDefsRef(name));
                } else if (subSchema instanceof JsonSchemaArray jsa) {
                    Type t = ((ListPsuedoSchema) ps).itemType();
                    TypeKey optionalItemKey = new TypeKey(t, direction);
                    String name = nameMap.get(optionalItemKey);
                    JsonSchema items = new JsonSchemaPrimitive(null, null, getDefsRef(name));
                    return new JsonSchemaArray("array", description, defs, items);
                } else if (subSchema instanceof JsonSchemaMap jsm) {
                    Type t = ((MapPsuedoSchema) ps).valueType();
                    TypeKey optionalItemKey = new TypeKey(t, direction);
                    String name = nameMap.get(optionalItemKey);
                    JsonSchema additionalProperties = new JsonSchemaPrimitive(null, null, getDefsRef(name));
                    return new JsonSchemaMap("object", description, defs, additionalProperties);

                }

            }

            return subSchema;
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
