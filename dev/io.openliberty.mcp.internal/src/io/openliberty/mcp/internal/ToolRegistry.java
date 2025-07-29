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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import io.openliberty.mcp.annotations.Tool;
import jakarta.enterprise.inject.spi.CDI;

public class ToolRegistry {

    private static ToolRegistry staticInstance = null;

    public static ToolRegistry get() {
        if (staticInstance != null) {
            return staticInstance;
        }
        return CDI.current().select(McpCdiExtension.class).get().getToolRegistry();
    }

    /**
     * For unit testing only
     *
     * @param toolRegistry
     */
    public static void set(ToolRegistry toolRegistry) {
        staticInstance = toolRegistry;
    }

    private Map<String, ToolMetadata> tools = new HashMap<>();

    public ToolMetadata getTool(String name) {
        ToolMetadata result = tools.get(name);
        if (result == null) {
            throw new NoSuchElementException(name);
        }
        return result;
    }

    public void addTool(ToolMetadata tool) {
        String name = tool.annotation().name();
        if (name.equals(Tool.ELEMENT_NAME)) {
            name = tool.method().getJavaMember().getName();
        }
        tools.put(name, tool);
    }

    public boolean hasTools() {
        return !tools.isEmpty();
    }

    public Collection<ToolMetadata> getAllTools() {
        return new ArrayList<>(tools.values());
    }

}
