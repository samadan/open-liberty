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

import io.openliberty.mcp.internal.schemas.PsuedoSchemaGenerator.JsonSchema;
import io.openliberty.mcp.internal.schemas.PsuedoSchemaGenerator.JsonSchemaArray;
import io.openliberty.mcp.internal.schemas.PsuedoSchemaGenerator.JsonSchemaEnum;
import io.openliberty.mcp.internal.schemas.PsuedoSchemaGenerator.JsonSchemaMap;
import io.openliberty.mcp.internal.schemas.PsuedoSchemaGenerator.JsonSchemaObject;
import io.openliberty.mcp.internal.schemas.PsuedoSchemaGenerator.JsonSchemaPrimitive;

/**
 *
 */
public class SchemaUtils {
    public static JsonSchema addDescriptionToJsonSchema(JsonSchema schema, String description) {
        if (schema instanceof JsonSchemaObject jso) {
            return new JsonSchemaObject(jso.type(), description, jso.properties(), jso.required(), jso.defs(), jso.ref());
        } else if (schema instanceof JsonSchemaArray jsa) {
            return new JsonSchemaArray(jsa.type(), description, jsa.defs(), jsa.items());
        } else if (schema instanceof JsonSchemaMap jsm) {
            return new JsonSchemaMap(jsm.type(), jsm.description() != null ? description : null, jsm.defs(), jsm.propertyNames(), jsm.additionalProperties());
        } else if (schema instanceof JsonSchemaEnum jse) {
            return new JsonSchemaEnum(jse.type(), description, jse.enumList());
        } else if (schema instanceof JsonSchemaPrimitive jsp) {
            return new JsonSchemaPrimitive(jsp.type(), description, jsp.ref());
        }
        return schema;
    }

}
