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

import io.openliberty.mcp.annotations.Tool;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessManagedBean;

/**
 * Finds tools
 */
public class McpCdiExtension implements Extension {

    private ToolRegistry tools = new ToolRegistry();

    void registerTools(@Observes ProcessManagedBean<?> pmb) {
        // TODO: limit this to just bean types with Tool annotations?
        AnnotatedType<?> type = pmb.getAnnotatedBeanClass();
        for (AnnotatedMethod<?> m : type.getMethods()) {
            Tool toolAnnotation = m.getAnnotation(Tool.class);
            if (toolAnnotation != null) {
                registerTool(toolAnnotation, pmb.getBean(), m);
            }
        }
    }

    private void registerTool(Tool tool, Bean<?> bean, AnnotatedMethod<?> method) {
        tools.addTool(ToolMetadata.createFrom(tool, bean, method));
    }

    public ToolRegistry getToolRegistry() {
        return tools;
    }
}
