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
import java.util.Optional;

import io.openliberty.mcp.annotations.Schema;
import io.openliberty.mcp.internal.schemas.SchemaGenerator.TypeKey;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;

/**
 *
 */
public class PsuedoSchemaGenerator {
    private static Jsonb jsonb = JsonbBuilder.create();

    public static ClassPsuedoSchema generateClassPsuedoSchema(AnnotatedType type, SchemaDirection direction) {
        List<FieldInfo> fields = null;
        Class<?> cls = (Class<?>) type.getType();
        switch (direction) {
            case INPUT -> fields = getInputFields(cls);
            case OUTPUT -> fields = getOutputFields(cls);
        }
        type.getAnnotations();
        return new ClassPsuedoSchema(cls, fields, type.getAnnotations() != null ? type.getAnnotations() : new Annotation[] {});

    }

    public static ClassPsuedoSchema generateBaseClassPsuedoSchema(Class<?> cls, SchemaDirection direction) {
        List<FieldInfo> fields = null;
        switch (direction) {
            case INPUT -> fields = getInputFields(cls);
            case OUTPUT -> fields = getOutputFields(cls);
            case INPUT_OUTPUT -> fields = getExplicitDefinedFields(cls);
        }
        return new ClassPsuedoSchema(cls, fields, new Annotation[] {});
    }

    public static ClassPsuedoSchema generateRecordPsuedoSchema(AnnotatedType type) {
        Class<?> cls = (Class<?>) type.getType();
        List<FieldInfo> fields = getExplicitDefinedFields(cls);
        return new ClassPsuedoSchema(cls, fields, type.getAnnotations() != null ? type.getAnnotations() : new Annotation[] {});
    }

    public static EnumPsuedoSchema generateEnumPsuedoSchema(AnnotatedType type) {
        Class<?> cls = (Class<?>) type.getType();
        List<String> enumValues = getEnumConstants(cls);
        return new EnumPsuedoSchema(type, enumValues);
    }

    public static ListPsuedoSchema generateRawCollectionPsuedoSchema(AnnotatedType type) {
        return new ListPsuedoSchema(type, null);
    }

    public static ListPsuedoSchema generateArrayPsuedoSchema(AnnotatedType type) {
        AnnotatedType elementType = ((AnnotatedArrayType) type).getAnnotatedGenericComponentType();
        return new ListPsuedoSchema(type, elementType);
    }

    public static MapPsuedoSchema generateRawMapPsuedoSchema(AnnotatedType type) {
        return new MapPsuedoSchema(type, null, null);
    }

    public static OptionalPsuedoSchema generateRawOptionalPsuedoSchema(AnnotatedType type) {
        return new OptionalPsuedoSchema(type, null);
    }

    public static ListPsuedoSchema generateParameterizedCollectionPsuedoSchema(AnnotatedType type) {
        AnnotatedType elementType = ((AnnotatedParameterizedType) type).getAnnotatedActualTypeArguments()[0];
        return new ListPsuedoSchema(type, elementType);
    }

    public static MapPsuedoSchema generateParameterizedMapPsuedoSchema(AnnotatedType type) {
        AnnotatedType[] elementTypes = ((AnnotatedParameterizedType) type).getAnnotatedActualTypeArguments();
        if (elementTypes.length == 2) {
            if (elementTypes[0].getType().equals(String.class) || elementTypes[0].getClass().isEnum()) {
                return new MapPsuedoSchema(type, elementTypes[0], elementTypes[1]);
            } else {
                throw new RuntimeException("Object keys not supported yet, provide custom schema instead");
            }
        } else {
            throw new RuntimeException("Processed invalid Map");
        }

    }

    public static OptionalPsuedoSchema generateParameterizedOptionalPsuedoSchema(AnnotatedType type) {
        AnnotatedType elementType = ((AnnotatedParameterizedType) type).getAnnotatedActualTypeArguments()[0];
        return new OptionalPsuedoSchema(type, elementType);
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
            if ((method.getReturnType() == null) && (method.getName().startsWith("set")) && method.getParameterCount() == 1) {
                String name = method.getAnnotation(JsonbProperty.class) != null ? method.getAnnotation(JsonbProperty.class).value() : method.getName().substring(3).toLowerCase();
                FieldInfo fi = new FieldInfo(name, method.getAnnotatedParameterTypes()[0],
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
            if ((method.getReturnType() != null) && method.getParameterCount() == 0 && (method.getName().startsWith("get"))) {
                String name = method.getAnnotation(JsonbProperty.class) != null ? method.getAnnotation(JsonbProperty.class).value() : method.getName().substring(3).toLowerCase();
                FieldInfo fi = new FieldInfo(name, method.getAnnotatedParameterTypes()[0],
                                             method.getDeclaredAnnotations() != null ? method.getDeclaredAnnotations() : new Annotation[] {}, SchemaDirection.OUTPUT);
                if (method.getAnnotation(JsonbTransient.class) == null)
                    result.add(fi);
            } else if ((method.getReturnType() == boolean.class) && method.getParameterCount() == 0 && (method.getName().startsWith("is"))) {
                String name = method.getAnnotation(JsonbProperty.class) != null ? method.getAnnotation(JsonbProperty.class).value() : method.getName().substring(2).toLowerCase();
                FieldInfo fi = new FieldInfo(name, method.getAnnotatedParameterTypes()[0],
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
                FieldInfo fi = new FieldInfo(name, field.getAnnotatedType(), field.getDeclaredAnnotations() != null ? field.getDeclaredAnnotations() : new Annotation[] {},
                                             SchemaDirection.INPUT_OUTPUT);
                if (field.getAnnotation(JsonbTransient.class) == null)
                    result.add(fi);
            } else if (Modifier.isPrivate(field.getModifiers()) && field.getAnnotation(JsonbProperty.class) != null) {
                String name = field.getAnnotation(JsonbProperty.class).value();
                FieldInfo fi = new FieldInfo(name, field.getAnnotatedType(), field.getDeclaredAnnotations() != null ? field.getDeclaredAnnotations() : new Annotation[] {},
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
                                             HashMap<String, JsonSchema> defs, boolean baseClass, String description);
    }

    public record ReadWriteDirectionStatus(boolean readable, boolean writeable) {}

    public record FieldInfo(String name, AnnotatedType type, Annotation[] annotations, SchemaDirection direction) {}

    public record ClassPsuedoSchema(Type baseType, List<FieldInfo> fields, Annotation[] annotations) implements PsuedoSchema {

        /** {@inheritDoc} */
        @Override
        public JsonSchema toJsonSchemaObject(SchemaDirection direction, HashMap<TypeKey, String> nameMap, HashMap<TypeKey, Boolean> typeFrequency,
                                             HashMap<String, JsonSchema> defs, boolean baseClass, String description) {

            TypeKey tmpKey = new TypeKey(baseType, direction);
            String name = nameMap.get(tmpKey);
            if (name == "company") {
                System.out.println("");
            }
            if (defs.containsKey(name) && defs.get(name) instanceof JsonSchema) {
                return new JsonSchemaPrimitive(null, description, getDefsRef(name));
            }
            Schema schemaAnn = (Schema) Arrays.stream(annotations()).filter(a -> a.annotationType().equals(Schema.class)).findAny().orElse(null);
            if (schemaAnn != null && schemaAnn.value() != null) {
                try {
                    JsonSchema resultObj = jsonb.fromJson(schemaAnn.value(), JsonSchemaObject.class);
                    return resultObj;
                } catch (JsonbException e) {
                    throw new RuntimeException("Schema annotation not valid: " + name);
                }
            }

            Map<TypeKey, PsuedoSchema> cache = getPsuedoCache();
            Map<String, JsonSchema> properties = new HashMap<>();
            List<String> required = new ArrayList<>();
            for (FieldInfo fi : fields()) {
                Schema childSchemaAnn = (Schema) Arrays.stream(fi.annotations()).filter(a -> a.annotationType().equals(Schema.class)).findAny().orElse(null);
                // get description
                String fiDescripton = childSchemaAnn != null ? childSchemaAnn.description() : null;

                TypeKey tmpTK = new TypeKey(fi.type().getType(), direction);
                PsuedoSchema ps = cache.get(tmpTK);
                if (!defs.containsKey(name)) {
                    defs.put(name, new JsonSchemaPlaceholder());
                }
                JsonSchema subSchema = ps.toJsonSchemaObject(direction, nameMap, typeFrequency, defs, false, fiDescripton);

                if (defs.containsKey(name) && defs.get(name) instanceof JsonSchemaPlaceholder) {
                    defs.remove(name);
                }

                properties.put(fi.name(), subSchema);
                if (fi.type() instanceof ParameterizedType && Optional.class.equals(fi.type())) {
                    continue;
                } else {
                    required.add(fi.name());
                }
            }
            boolean storeInDefs = typeFrequency.get(tmpKey);
            String schemaDescription = storeInDefs ? null : description;

            JsonSchema schema;
            if (baseClass && !defs.isEmpty() && typeFrequency.get(tmpKey) == false) {
                schema = new JsonSchemaObject("object", schemaDescription, properties, required, defs, null);
            } else {
                schema = new JsonSchemaObject("object", schemaDescription, properties, required, null, null);
            }

            if (typeFrequency.get(tmpKey) == true) {
                if (!defs.containsKey(name)) {
                    defs.put(name, schema);
                }
                if (baseClass == false) {
                    return new JsonSchemaPrimitive(null, description, getDefsRef(name));
                } else {
                    return new JsonSchemaObject(null, description, null, null, defs, getDefsRef(name));
                }

            }

            return schema;
        }
    }

    public record ListPsuedoSchema(AnnotatedType baseType, AnnotatedType itemType) implements PsuedoSchema {

        /** {@inheritDoc} */
        @Override
        public JsonSchema toJsonSchemaObject(SchemaDirection direction, HashMap<TypeKey, String> nameMap, HashMap<TypeKey, Boolean> typeFrequency,
                                             HashMap<String, JsonSchema> defs, boolean baseClass, String description) {

            TypeKey tmpKey = new TypeKey(baseType.getType(), direction);
            String name = nameMap.get(tmpKey);

            Schema schemaAnn = (Schema) Arrays.stream(itemType.getAnnotations()).filter(a -> a.annotationType().equals(Schema.class)).findAny().orElse(null);
            if (schemaAnn != null && schemaAnn.value() != null) {
                try {
                    JsonSchema resultObj = jsonb.fromJson(schemaAnn.value(), JsonSchemaObject.class);
                    return resultObj;
                } catch (JsonbException e) {
                    throw new RuntimeException("Schema annotation not valid: " + name);
                }
            }

            Map<TypeKey, PsuedoSchema> cache = getPsuedoCache();

            // get description
            String fiDescripton = schemaAnn != null ? schemaAnn.description() : null;

            TypeKey tmpTK = new TypeKey(itemType.getType(), direction);
            PsuedoSchema ps = cache.get(tmpTK);
            JsonSchema subSchema = ps.toJsonSchemaObject(direction, nameMap, typeFrequency, defs, false, fiDescripton);

            JsonSchema schema = new JsonSchemaArray("array", description, null, subSchema);

            if (typeFrequency.get(tmpKey) == true) {
                if (!defs.containsKey(name)) {
                    defs.put(name, schema);
                }
                if (baseClass == true) {
                    JsonSchema items = new JsonSchemaPrimitive(null, fiDescripton, getDefsRef(name));
                    return new JsonSchemaArray("array", "", defs, items);
                }
            }

            return schema;
        }

    }

    public record MapPsuedoSchema(AnnotatedType baseType, AnnotatedType keyType, AnnotatedType valueType) implements PsuedoSchema {

        /** {@inheritDoc} */
        @Override
        public JsonSchema toJsonSchemaObject(SchemaDirection direction, HashMap<TypeKey, String> nameMap, HashMap<TypeKey, Boolean> typeFrequency,
                                             HashMap<String, JsonSchema> defs, boolean baseClass, String description) {

            TypeKey tmpKey = new TypeKey(baseType.getType(), direction);
            String name = nameMap.get(tmpKey);

            Schema schemaAnn = (Schema) Arrays.stream(valueType.getAnnotations()).filter(a -> a.annotationType().equals(Schema.class)).findAny().orElse(null);
            if (schemaAnn != null && schemaAnn.value() != null) {
                try {
                    JsonSchema resultObj = jsonb.fromJson(schemaAnn.value(), JsonSchemaObject.class);
                    return resultObj;
                } catch (JsonbException e) {
                    throw new RuntimeException("Schema annotation not valid: " + name);
                }
            }

            Map<TypeKey, PsuedoSchema> cache = getPsuedoCache();

            // get description
            String fiDescripton = schemaAnn != null ? schemaAnn.description() : null;

            TypeKey tmpTK = new TypeKey(valueType.getType(), direction);
            PsuedoSchema ps = cache.get(tmpTK);
            JsonSchema valueSchema = ps.toJsonSchemaObject(direction, nameMap, typeFrequency, defs, false, fiDescripton);

            JsonSchemaMap schema = new JsonSchemaMap("object", null, null, valueSchema);

            if (typeFrequency.get(tmpKey) == true) {
                if (!defs.containsKey(name)) {
                    defs.put(name, schema);
                }
                if (baseClass == true) {
                    JsonSchema additionalProperties = new JsonSchemaPrimitive(null, null, getDefsRef(name));
                    return new JsonSchemaMap("object", null, defs, additionalProperties);
                }
            }

            return schema;
        }
    }

    public record EnumPsuedoSchema(AnnotatedType baseType, List<String> values) implements PsuedoSchema {

        /** {@inheritDoc} */
        @Override
        public JsonSchema toJsonSchemaObject(SchemaDirection direction, HashMap<TypeKey, String> nameMap, HashMap<TypeKey, Boolean> typeFrequency,
                                             HashMap<String, JsonSchema> defs, boolean baseClass, String description) {

            TypeKey tmpKey = new TypeKey(baseType.getType(), direction);
            String name = nameMap.get(tmpKey);

            Schema schemaAnn = (Schema) Arrays.stream(baseType.getAnnotations()).filter(a -> a.annotationType().equals(Schema.class)).findAny().orElse(null);
            if (schemaAnn != null && schemaAnn.value() != null) {
                try {
                    JsonSchema resultObj = jsonb.fromJson(schemaAnn.value(), JsonSchemaObject.class);
                    return resultObj;
                } catch (JsonbException e) {
                    throw new RuntimeException("Schema annotation not valid: " + name);
                }
            }

            Map<TypeKey, PsuedoSchema> cache = getPsuedoCache();
            // get description
            String fiDescripton = schemaAnn != null ? schemaAnn.description() : null;

            PsuedoSchema ps = cache.get(tmpKey);
            JsonSchema valueSchema = ps.toJsonSchemaObject(direction, nameMap, typeFrequency, defs, false, fiDescripton);

            JsonSchemaEnum schema = new JsonSchemaEnum("String", fiDescripton, values());

            if (typeFrequency.get(tmpKey) == true) {
                if (!defs.containsKey(tmpKey)) {
                    defs.put(name, schema);
                }
            }

            return schema;
        }
    }

    public record PrimitivePsuedoSchema(AnnotatedType baseType, Class<?> primitiveType) implements PsuedoSchema {
        /** {@inheritDoc} */
        @Override
        public JsonSchema toJsonSchemaObject(SchemaDirection direction, HashMap<TypeKey, String> nameMap, HashMap<TypeKey, Boolean> typeFrequency,
                                             HashMap<String, JsonSchema> defs, boolean baseClass, String description) {

            Type type = baseType.getType();
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

    public record OptionalPsuedoSchema(AnnotatedType baseType, AnnotatedType optionalType) implements PsuedoSchema {
        /** {@inheritDoc} */
        @Override
        public JsonSchema toJsonSchemaObject(SchemaDirection direction, HashMap<TypeKey, String> nameMap, HashMap<TypeKey, Boolean> typeFrequency,
                                             HashMap<String, JsonSchema> defs, boolean baseClass, String description) {

            TypeKey tmpKey = new TypeKey(baseType.getType(), direction);
            String name = nameMap.get(tmpKey);

            Schema schemaAnn = (Schema) Arrays.stream(optionalType.getAnnotations()).filter(a -> a.annotationType().equals(Schema.class)).findAny().orElse(null);
            if (schemaAnn != null && schemaAnn.value() != null) {
                try {
                    JsonSchema resultObj = jsonb.fromJson(schemaAnn.value(), JsonSchemaObject.class);
                    return resultObj;
                } catch (JsonbException e) {
                    throw new RuntimeException("Schema annotation not valid: " + name);
                }
            }

            Map<TypeKey, PsuedoSchema> cache = getPsuedoCache();

            // get description
            String fiDescripton = schemaAnn != null ? schemaAnn.description() : null;

            TypeKey tmpTK = new TypeKey(optionalType.getType(), direction);
            PsuedoSchema ps = cache.get(tmpTK);
            JsonSchema subSchema = ps.toJsonSchemaObject(direction, nameMap, typeFrequency, defs, false, fiDescripton);

            if (typeFrequency.get(tmpKey) == true) {
                if (!defs.containsKey(name)) {
                    defs.put(name, subSchema);
                }
                if (subSchema instanceof JsonSchemaObject jso) {
                    if (baseClass == false) {
                        return new JsonSchemaPrimitive(null, null, getDefsRef(name));
                    } else {
                        return new JsonSchemaObject(null, null, null, null, defs, getDefsRef(name));
                    }
                } else if (subSchema instanceof JsonSchemaArray jsa) {
                    if (baseClass == true) {
                        JsonSchema items = new JsonSchemaPrimitive(null, fiDescripton, getDefsRef(name));
                        return new JsonSchemaArray("array", null, defs, items);
                    }
                } else if (subSchema instanceof JsonSchemaMap jsm) {
                    if (baseClass == true) {
                        JsonSchema additionalProperties = new JsonSchemaPrimitive(null, null, getDefsRef(name));
                        return new JsonSchemaMap("object", null, defs, additionalProperties);
                    }
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
