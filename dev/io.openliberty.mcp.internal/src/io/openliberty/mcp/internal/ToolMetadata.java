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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.annotations.WrapBusinessError;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.Bean;

/**
 *
 */
public record ToolMetadata(Tool annotation, Bean<?> bean, AnnotatedMethod<?> method,
                           Map<String, ArgumentMetadata> arguments,
                           List<SpecialArgumentMetadata> specialArguments,
                           String name, String title, String description,
                           List<Class<? extends Throwable>> businessExceptions) {

    public enum ToolArgStatus {
        BLANK,
        DUPLICATE,
        PASSED_VALIDATION
    }

    public record ArgumentMetadata(Type type, int index, String description, boolean required, ToolArgStatus toolArgumentStatus) {}

    public record SpecialArgumentMetadata(SpecialArgumentType type, int index) {}

    public ToolMetadata {
        arguments = ((arguments == null) ? Collections.emptyMap() : arguments);
        specialArguments = ((specialArguments == null) ? Collections.emptyList() : specialArguments);
    }

    public static ToolMetadata createFrom(Tool annotation, Bean<?> bean, AnnotatedMethod<?> method) {

        String name = annotation.name().equals(Tool.ELEMENT_NAME) ? method.getJavaMember().getName() : annotation.name();
        String title = annotation.title().isEmpty() ? null : annotation.title();
        String description = annotation.description().isEmpty() ? null : annotation.description();

        WrapBusinessError wrapAnnotation = method.getAnnotation(WrapBusinessError.class);
        List<Class<? extends Throwable>> businessExceptions = (wrapAnnotation != null) ? List.of(wrapAnnotation.value()) : Collections.emptyList();

        return new ToolMetadata(annotation, bean, method, getArgumentMap(method), getSpecialArgumentList(method), name, title, description, businessExceptions);
    }

    private static Map<String, ArgumentMetadata> getArgumentMap(AnnotatedMethod<?> method) {
        Map<String, ArgumentMetadata> result = new HashMap<>();
        Set<String> argNamesDupCheck = new HashSet<>();

        for (AnnotatedParameter<?> p : method.getParameters()) {
            ToolArg pInfo = p.getAnnotation(ToolArg.class);
            if (pInfo != null) {
                setArgumentData(method, result, p, pInfo, argNamesDupCheck);
            }
        }
        return result.isEmpty() ? Collections.emptyMap() : result;
    }

    private static void setArgumentData(AnnotatedMethod<?> method, Map<String, ArgumentMetadata> result, AnnotatedParameter<?> p, ToolArg pInfo, Set<String> argNamesDupCheck) {
        String actualArgName = p.getJavaParameter().getName(); // needs a gradle "-parameter" compilation flag to work
        String toolArgName = pInfo.name();
        Type type = p.getBaseType();
        int position = p.getPosition();
        String description = pInfo.description();
        boolean required = pInfo.required();

        if (toolArgName.equals(Tool.ELEMENT_NAME)) {
            result.put(actualArgName, new ArgumentMetadata(type, position, description, required, ToolArgStatus.PASSED_VALIDATION));
        } else if (toolArgName.isBlank()) {
            result.put(toolArgName, new ArgumentMetadata(type, position, description, required, ToolArgStatus.BLANK));
        } else {
            if (!argNamesDupCheck.add(toolArgName)) {
                result.put(toolArgName, new ArgumentMetadata(type, position, description, required, ToolArgStatus.DUPLICATE));
            } else {
                result.put(toolArgName, new ArgumentMetadata(type, position, description, required, ToolArgStatus.PASSED_VALIDATION));
            }
        }
    }

    private static List<SpecialArgumentMetadata> getSpecialArgumentList(AnnotatedMethod<?> method) {
        List<SpecialArgumentMetadata> result = new ArrayList<>();
        for (AnnotatedParameter<?> p : method.getParameters()) {
            ToolArg pInfo = p.getAnnotation(ToolArg.class);
            if (pInfo == null) {
                SpecialArgumentMetadata pData = new SpecialArgumentMetadata(SpecialArgumentType.fromClass(p.getBaseType()), p.getPosition());
                result.add(pData);
            }
        }
        return Collections.unmodifiableList(result);
    }
}
