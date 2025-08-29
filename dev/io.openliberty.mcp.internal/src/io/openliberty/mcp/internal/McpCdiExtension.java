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

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import io.openliberty.mcp.annotations.Tool;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessManagedBean;

/**
 * Finds tools
 */
public class McpCdiExtension implements Extension {

    private ToolRegistry tools = new ToolRegistry();
    private ConcurrentHashMap<String, LinkedList<String>> duplicateToolsMap = new ConcurrentHashMap<>();

    void registerTools(@Observes ProcessManagedBean<?> pmb) {
        // TODO: limit this to just bean types with Tool annotations?
        AnnotatedType<?> type = pmb.getAnnotatedBeanClass();
        Class<?> javaClass = type.getJavaClass();
        for (AnnotatedMethod<?> m : type.getMethods()) {
            Tool toolAnnotation = m.getAnnotation(Tool.class);
            if (toolAnnotation != null) {
                registerTool(toolAnnotation, pmb.getBean(), m, javaClass.getPackageName() + "." + javaClass.getSimpleName());
            }
        }

        // prune items that are not duplicates
        duplicateToolsMap.entrySet().removeIf(e -> e.getValue().size() == 1);
    }

    void afterDeploymentValidation(@Observes AfterDeploymentValidation afterDeploymentValidation, BeanManager manager) {

        StringBuilder sb = new StringBuilder();

        for (String toolName : duplicateToolsMap.keySet()) {
            LinkedList<String> qualifiedNames = duplicateToolsMap.get(toolName);

            sb.append("Tool: ").append(toolName).append("\n");
            sb.append("  Qualified Names:\n");
            for (String name : qualifiedNames) {
                sb.append("    - ").append(name).append("\n");
            }
        }

        if (duplicateToolsMap.size() != 0) {
            afterDeploymentValidation.addDeploymentProblem(new Exception("More than one MCP tool has the name: " + sb.toString()));
        }
    }

    private void registerTool(Tool tool, Bean<?> bean, AnnotatedMethod<?> method, String qualifiedName) {
        ToolMetadata toolmd = ToolMetadata.createFrom(tool, bean, method);
        duplicateToolsMap.computeIfAbsent(toolmd.name(), key -> new LinkedList<>()).add(qualifiedName);
        tools.addTool(toolmd);
    }

    public ToolRegistry getToolRegistry() {
        return tools;
    }
}
