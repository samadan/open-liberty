/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.Bean;

/**
 *
 */

public record ToolMetadata(Tool.Annotations annotations, Tool annotation, Bean<?> bean, AnnotatedMethod<?> method,
                           Map<String, ArgumentMetadata> arguments, String name, String title, String description) {

    public record ArgumentMetadata(Type type, int index, String description) {}

    public static ToolMetadata createFrom(Tool annotation, Bean<?> bean, AnnotatedMethod<?> method) {

        String name = annotation.name().equals(Tool.ELEMENT_NAME) ? method.getJavaMember().getName() : annotation.name();
        String title = annotation.title().isEmpty() ? null : annotation.title();
        String description = annotation.description().isEmpty() ? null : annotation.description();

        return new ToolMetadata(annotation.annotations(), annotation, bean, method, getArgumentMap(method), name, title, description);
    }

    private static Map<String, ArgumentMetadata> getArgumentMap(AnnotatedMethod<?> method) {
        Map<String, ArgumentMetadata> result = new HashMap<>();
        for (AnnotatedParameter<?> p : method.getParameters()) {
            ToolArg pInfo = p.getAnnotation(ToolArg.class);
            if (pInfo != null) {
                ArgumentMetadata pData = new ArgumentMetadata(p.getBaseType(), p.getPosition(), pInfo.description());
                if (pInfo.name().equals(Tool.ELEMENT_NAME)) {
                    result.put(method.getJavaMember().getName(), pData);
                } else {
                    result.put(pInfo.name(), pData);
                }
            }
        }
        return result;
    }
}
