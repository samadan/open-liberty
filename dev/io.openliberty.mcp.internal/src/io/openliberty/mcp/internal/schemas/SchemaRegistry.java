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
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.openliberty.mcp.annotations.Schema;
import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
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

    private static HashMap<SchemaKey, String> cacheString = new HashMap<>();

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

    public static String generateSchema(Class<?> cls) {
        ClassKey ck = new ClassKey(cls);
        if (!cacheString.containsKey(ck)) {
            return generateSchema(cls, null);
        }
        return cacheString.get(ck);
    }

    private static String generateSchema(Class<?> cls, String description) {

        ClassKey ck = new ClassKey(cls);
        HashMap<SchemaKey, CacheEntryObject> cacheObject = new HashMap<>();
        HashMap<SchemaKey, List<ParentSchemaMapNode>> tally = new HashMap<>();
        HashMap<String, Integer> nameGenerator = new HashMap<>();
        HashMap<SchemaKey, String> nameMap = new HashMap<>();
        HashMap<String, Object> defs = new HashMap<>();

        Object schemaObj = generateSchemaCache(cls, cacheObject, description, tally, nameGenerator, nameMap, null, null);
        schemaObj = duplicateSchemaHandler(schemaObj, cacheObject, description, tally, nameMap, defs);
        cacheString.put(ck, jsonb.toJson(schemaObj));

        return cacheString.get(ck);

    }

    private static String getDefsRef(String name) {
        return "#/$defs/" + name;
    }

    public static String generateSchema(Method method) {
        MethodKey mk = new MethodKey(method);
        if (!cacheString.containsKey(mk)) {
            HashMap<SchemaKey, CacheEntryObject> cacheObject = new HashMap<>();
            HashMap<SchemaKey, List<ParentSchemaMapNode>> tally = new HashMap<>();
            HashMap<String, Integer> nameGenerator = new HashMap<>();
            HashMap<SchemaKey, String> nameMap = new HashMap<>();
            HashMap<String, Object> defs = new HashMap<>();
            Map<String, Object> schemaObjPropertiesMap = new HashMap<>();
            LinkedList<String> requiredParameterList = new LinkedList<>();
            for (Parameter p : method.getParameters()) {
                ToolArg pInfo = p.getAnnotation(ToolArg.class);
                Object schemaObj = paramSchemaGenerator(p, cacheObject, pInfo.description(), tally, nameGenerator, nameMap, schemaObjPropertiesMap, pInfo.name());

                schemaObjPropertiesMap.put(pInfo.name(), schemaObj);
                requiredParameterList.add(pInfo.name());
            }
            for (Map.Entry<SchemaKey, List<ParentSchemaMapNode>> entry : tally.entrySet()) {
                List<ParentSchemaMapNode> v = entry.getValue();
                SchemaKey k = entry.getKey();

                if (v.size() > 1) {
                    String name = nameMap.get(k);

                    for (ParentSchemaMapNode parentMapNode : v) {
                        Map<String, Object> parentSchemaMap = parentMapNode.parentSchemaMap();
                        String parentKey = parentMapNode.parentSchemaKey();
                        Object type = parentSchemaMap.get(parentKey);
                        Object parentType = type;
                        while (type instanceof JsonSchemaArray) {
                            parentType = type;
                            type = ((JsonSchemaArray) type).getItems();
                        }
                        if (parentType instanceof JsonSchemaArray) {
                            ((JsonSchemaArray) parentType).setItems(new JsonSchemaPrimitive(null, null, getDefsRef(name)));
                        } else {
                            parentSchemaMap.put(parentKey, new JsonSchemaPrimitive(null, null, getDefsRef(name)));
                        }

                        if (!defs.containsKey(name)) {
                            defs.put(name, cacheObject.get(k).getObj());
                        }
                    }
                }
            }
            String methodDescripton = method.getAnnotation(Tool.class) != null ? method.getAnnotation(Tool.class).description() : null;
            JsonSchemaObject schemaObj = new JsonSchemaObject("object", methodDescripton, schemaObjPropertiesMap, requiredParameterList, defs.keySet().size() > 0 ? defs : null,
                                                              null);
            cacheString.put(mk, jsonb.toJson(schemaObj));
        }
        return cacheString.get(mk);
    }

    public static String generateSchema(Parameter param) {

        ParamKey pk = new ParamKey(param);
        HashMap<SchemaKey, CacheEntryObject> cacheObject = new HashMap<>();
        HashMap<SchemaKey, List<ParentSchemaMapNode>> tally = new HashMap<>();
        HashMap<String, Integer> nameGenerator = new HashMap<>();
        HashMap<SchemaKey, String> nameMap = new HashMap<>();
        HashMap<String, Object> defs = new HashMap<>();

        ToolArg pInfo = param.getAnnotation(ToolArg.class);
        Object schemaObj = paramSchemaGenerator(param, cacheObject, pInfo.description(), tally, nameGenerator, nameMap, null, null);
        schemaObj = duplicateSchemaHandler(schemaObj, cacheObject, pInfo.description(), tally, nameMap, defs);
        cacheString.put(pk, jsonb.toJson(schemaObj));
        return cacheString.get(pk);
    }

    private static Object duplicateSchemaHandler(Object schemaObj, HashMap<SchemaKey, CacheEntryObject> cacheObject, String description,
                                                 HashMap<SchemaKey, List<ParentSchemaMapNode>> tally,
                                                 HashMap<SchemaKey, String> nameMap, HashMap<String, Object> defs) {
        for (Map.Entry<SchemaKey, List<ParentSchemaMapNode>> entry : tally.entrySet()) {
            List<ParentSchemaMapNode> v = entry.getValue();
            SchemaKey k = entry.getKey();

            if (v.size() > 1) {
                String name = nameMap.get(k);

                for (ParentSchemaMapNode parentMapNode : v) {
                    if (parentMapNode.parentSchemaMap == null) {
                        if (schemaObj instanceof JsonSchemaArray) {
                            JsonSchemaArray schemaObjTmp = (JsonSchemaArray) schemaObj;
                            schemaObjTmp.setItems(new JsonSchemaPrimitive(null, null, getDefsRef(name)));
                            schemaObjTmp.setDefs(defs);
                            schemaObj = schemaObjTmp;
                        } else {
                            schemaObj = new JsonSchemaObject(null, null, null, null, defs, getDefsRef(name));
                        }

                    } else {
                        Map<String, Object> parentSchemaMap = parentMapNode.parentSchemaMap();
                        String parentKey = parentMapNode.parentSchemaKey();
                        Object type = parentSchemaMap.get(parentKey);
                        Object parentType = type;
                        while (type instanceof JsonSchemaArray) {
                            parentType = type;
                            type = ((JsonSchemaArray) type).getItems();
                        }
                        if (parentType instanceof JsonSchemaArray) {
                            ((JsonSchemaArray) parentType).setItems(new JsonSchemaPrimitive(null, null, getDefsRef(name)));
                        } else {
                            parentSchemaMap.put(parentKey, new JsonSchemaPrimitive(null, null, getDefsRef(name)));
                        }
                    }
                    if (!defs.containsKey(name)) {
                        defs.put(name, cacheObject.get(k).getObj());
                    }
                }
            }
        }
        if (schemaObj instanceof JsonSchemaObject) {
            JsonSchemaObject SchemaObjTmp = (JsonSchemaObject) schemaObj;
            schemaObj = new JsonSchemaObject(SchemaObjTmp.type(), null, SchemaObjTmp.properties(), SchemaObjTmp.required(), defs.keySet().size() > 0 ? defs : null,
                                             SchemaObjTmp.ref());
        }
        return schemaObj;
    }

    private static Object paramSchemaGenerator(Parameter param, HashMap<SchemaKey, CacheEntryObject> cacheObject, String description,
                                               HashMap<SchemaKey, List<ParentSchemaMapNode>> tally,
                                               HashMap<String, Integer> nameGenerator,
                                               HashMap<SchemaKey, String> nameMap, Map<String, Object> parentSchemaMap, String parentSchemaKey) {
        ToolArg pInfo = param.getAnnotation(ToolArg.class);
        if (pInfo != null) {
            Type type = param.getParameterizedType();
            Object schemaObj;
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                if (parameterizedType.getRawType().equals(Map.class)) {
                    schemaObj = new JsonSchemaPrimitive("object", pInfo.description(), null);
                } else {
                    Type subtype = parameterizedType.getActualTypeArguments()[0];
                    schemaObj = new JsonSchemaArray("array", pInfo.description(),
                                                    generateSchemaCache((Class<?>) subtype, cacheObject, null, tally, nameGenerator, nameMap,
                                                                        parentSchemaMap, parentSchemaKey),
                                                    null);
                }

            } else if (type.equals(Array.class)) {
                schemaObj = new JsonSchemaArray("array", pInfo.description(),
                                                generateSchemaCache(((Class<?>) type).getComponentType(), cacheObject, null, tally, nameGenerator, nameMap,
                                                                    parentSchemaMap, parentSchemaKey),
                                                null);
            } else {
                schemaObj = generateSchemaCache((Class<?>) type, cacheObject, pInfo.description(), tally, nameGenerator, nameMap, parentSchemaMap, parentSchemaKey);
            }
            return schemaObj;
        } else
            throw new IllegalArgumentException("non annotated param not supported");
    }

    private static Object generateSchemaCache(Class<?> cls, HashMap<SchemaKey, CacheEntryObject> cacheObject, String description,
                                              HashMap<SchemaKey, List<ParentSchemaMapNode>> tally,
                                              HashMap<String, Integer> nameGenerator,
                                              HashMap<SchemaKey, String> nameMap, Map<String, Object> parentSchemaMap, String parentSchemaKey) {
        ClassKey ck = new ClassKey(cls);
        if (!cacheObject.containsKey(ck)) {
            cacheObject.put(ck, new CacheEntryObject(null, false));
            String name;
            if (nameGenerator.containsKey(cls.getSimpleName())) {
                nameGenerator.compute(cls.getSimpleName(), (k, v) -> v + 1);
                name = cls.getSimpleName() + nameGenerator.get(cls.getSimpleName());
            } else {
                nameGenerator.put(cls.getSimpleName(), 1);
                name = cls.getSimpleName();
            }
            nameMap.put(ck, name);
            JsonObjectBuilder jsonObject = Json.createObjectBuilder();

            if (cls.isAnnotationPresent(Schema.class)) {
                List<ParentSchemaMapNode> listTmp = tally.getOrDefault(ck, new ArrayList<>());
                listTmp.add(new ParentSchemaMapNode(parentSchemaMap, parentSchemaKey));
                tally.put(ck, listTmp);

                Schema schema = cls.getAnnotation(Schema.class);
                try {
                    JsonSchemaObject resultObj = jsonb.fromJson(schema.value(), JsonSchemaObject.class);
                    CacheEntryObject entry = cacheObject.get(ck);
                    entry.setObj(resultObj);
                    entry.setComplete(true);
                    return resultObj;
                } catch (JsonbException e) {
                    throw new RuntimeException("Schema annotation not valid: " + cls.getName());
                }

            } else {

                Map<String, Object> schemaMap = new HashMap<>();
                LinkedList<String> requiredParameterList = new LinkedList<>();
                Field[] fields = cls.getDeclaredFields();

                List<ParentSchemaMapNode> listTmp = tally.getOrDefault(ck, new ArrayList<>());
                listTmp.add(new ParentSchemaMapNode(parentSchemaMap, parentSchemaKey));
                tally.put(ck, listTmp);

                for (Field field : fields) {

                    if (field.getAnnotation(JsonbTransient.class) == null) {
                        String argumentName = field.getAnnotation(JsonbProperty.class) != null ? field.getAnnotation(JsonbProperty.class).value() : field.getName();
                        Type subtype = field.getGenericType();
                        while (subtype instanceof ParameterizedType) {
                            ParameterizedType parameterizedType = (ParameterizedType) subtype;
                            subtype = parameterizedType.getActualTypeArguments()[0];
                        }
                        Class<?> fieldClass = (Class<?>) subtype;
                        ClassKey tmpKeyCheck = new ClassKey(fieldClass);
                        schemaMap.put(argumentName, buildSchema(field, cacheObject, tally, nameGenerator, nameMap, schemaMap, argumentName));

                        requiredParameterList.add(argumentName);
                    }

                }
                JsonSchemaObject resultObj = new JsonSchemaObject("object", description, schemaMap, requiredParameterList, null, null);
                CacheEntryObject entry = cacheObject.get(ck);
                entry.setObj(resultObj);
                entry.setComplete(true);
                return resultObj;

            }

        } else {
            List<ParentSchemaMapNode> listTmp = tally.getOrDefault(ck, new ArrayList<>());
            listTmp.add(new ParentSchemaMapNode(parentSchemaMap, parentSchemaKey));
            tally.put(ck, listTmp);
            if (cacheObject.get(ck).isComplete() != false) {
                if (description != null) {
                    CacheEntryObject tmpCacheObj = cacheObject.get(ck);
                    Object tmp = tmpCacheObj.getObj();
                    if (tmp instanceof JsonSchemaObject) {
                        JsonSchemaObject tmpJson = (JsonSchemaObject) tmp;
                        tmpCacheObj.setObj(new JsonSchemaObject(tmpJson.type(), description, tmpJson.properties(), tmpJson.required(), tmpJson.defs(), tmpJson.ref()));
                    }
                }
                return cacheObject.get(ck).getObj();

            } else {
                return cacheObject.get(ck);
            }
        }

    }

    private static Object buildSchema(Field field, HashMap<SchemaKey, CacheEntryObject> cacheObject, HashMap<SchemaKey, List<ParentSchemaMapNode>> tally,
                                      HashMap<String, Integer> nameGenerator, HashMap<SchemaKey, String> nameMap,
                                      Map<String, Object> parentSchemaMap, String parentSchemaKey) {
        Object tempSchema;
        if (!field.isAnnotationPresent(Schema.class)) {
            Type type = field.getGenericType();
            String description = null;

            boolean isJsonNumber = type.equals(long.class) || type.equals(double.class) || type.equals(byte.class) || type.equals(float.class) || type.equals(short.class)
                                   || type.equals(Long.class) || type.equals(Double.class) || type.equals(Byte.class) || type.equals(Float.class) || type.equals(Short.class);

            if (isJsonNumber)
                tempSchema = new JsonSchemaPrimitive("number", description, null);
            else if (type.equals(String.class) || type.equals(Character.class) || type.equals(char.class))
                tempSchema = new JsonSchemaPrimitive("string", description, null);
            else if (type.equals(int.class) || type.equals(Integer.class))
                tempSchema = new JsonSchemaPrimitive("integer", description, null);
            else if (type.equals(boolean.class) || type.equals(Boolean.class))
                tempSchema = new JsonSchemaPrimitive("boolean", description, null);
            else if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                if (parameterizedType.getRawType().equals(Map.class)) {
                    tempSchema = new JsonSchemaPrimitive("object", description, null);
                } else {
                    Type subtype = parameterizedType.getActualTypeArguments()[0];
                    tempSchema = new JsonSchemaArray("array", description,
                                                     generateSchemaCache((Class<?>) subtype, cacheObject, description, tally, nameGenerator, nameMap, parentSchemaMap,
                                                                         parentSchemaKey),
                                                     null);
                }

            } else if (type.equals(Array.class)) {
                tempSchema = new JsonSchemaArray("array", description,
                                                 generateSchemaCache(field.getType().getComponentType(), cacheObject, description, tally, nameGenerator, nameMap, parentSchemaMap,
                                                                     parentSchemaKey),
                                                 null);
            } else {
                tempSchema = generateSchemaCache((Class<?>) type, cacheObject, description, tally, nameGenerator, nameMap, parentSchemaMap, parentSchemaKey);
            }

        } else {
            Schema schema = field.getAnnotation(Schema.class);
            try {
                return jsonb.fromJson(schema.value(), JsonSchemaObject.class);
            } catch (JsonbException e) {
                throw new RuntimeException("Schema annotation not valid for feild: " + field.getType().getTypeName());
            }
        }
        return tempSchema;

    }

    public record JsonSchemaObject(String type, String description, Map<String, Object> properties, List<String> required, @JsonbProperty("$defs") HashMap<String, Object> defs,
                                   @JsonbProperty("$ref") String ref) {}

    public static class JsonSchemaArray {
        String type;
        String description;
        Object items;
        HashMap<String, Object> defs;

        public JsonSchemaArray(String type, String description, Object items, @JsonbProperty("$defs") HashMap<String, Object> defs) {
            this.type = type;
            this.description = description;
            this.items = items;
            this.defs = defs;
        }

        /**
         * @return the type
         */
        public String getType() {
            return type;
        }

        /**
         * @param type the type to set
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * @return the description
         */
        public String getDescription() {
            return description;
        }

        /**
         * @param description the description to set
         */
        public void setDescription(String description) {
            this.description = description;
        }

        /**
         * @return the items
         */
        public Object getItems() {
            return items;
        }

        /**
         * @param items the items to set
         */
        public void setItems(Object items) {
            this.items = items;
        }

        /**
         * @return the defs
         */
        public HashMap<String, Object> getDefs() {
            return defs;
        }

        /**
         * @param defs the defs to set
         */
        public void setDefs(HashMap<String, Object> defs) {
            this.defs = defs;
        }

    }

//    public record JsonSchemaArray(String type, String description, Object items) {}

    public record JsonSchemaPrimitive(String type, String description, @JsonbProperty("$ref") String ref) {}

    public record CacheObject(Object obj, String schema) {}

    public record ParentSchemaMapNode(Map<String, Object> parentSchemaMap, String parentSchemaKey) {}

    public static class CacheEntryObject {
        private Object obj;
        private boolean complete;

        /**
         * @param obj
         * @param complete
         */
        public CacheEntryObject(Object obj, boolean complete) {
            super();
            this.obj = obj;
            this.complete = complete;
        }

        /**
         * @return the obj
         */
        public Object getObj() {
            return obj;
        }

        /**
         * @param obj the obj to set
         */
        public void setObj(Object obj) {
            this.obj = obj;
        }

        /**
         * @return the complete
         */
        public boolean isComplete() {
            return complete;
        }

        /**
         * @param complete the complete to set
         */
        public void setComplete(boolean complete) {
            this.complete = complete;
        }

    }

    public interface SchemaKey {}

    public record ClassKey(Class<?> cls) implements SchemaKey {
        @Override
        public boolean equals(Object obj) {
            ClassKey givenClass = (ClassKey) obj;
            return givenClass.cls().getCanonicalName() == cls.getCanonicalName();

        }

        @Override
        public int hashCode() {
            return Objects.hash(cls.getCanonicalName());
        }

    };

    public record MethodKey(Method method) implements SchemaKey {};

    public record ParamKey(Parameter param) implements SchemaKey {};

}
