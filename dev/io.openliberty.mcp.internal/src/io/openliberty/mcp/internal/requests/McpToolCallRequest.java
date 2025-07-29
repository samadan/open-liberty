/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.requests;

import java.lang.reflect.Method;

import io.openliberty.mcp.internal.RequestMethod;
import io.openliberty.mcp.internal.ToolMetadata;
import jakarta.enterprise.inject.spi.Bean;

/**
 *
 */
public class McpToolCallRequest extends McpRequest {

    /**
     * @param id
     * @param requestMethod
     * @param method
     * @param arguments
     */
    public McpToolCallRequest(String id, RequestMethod requestMethod, ToolMetadata metadata, Object[] arguments) {
        super(id, requestMethod);
        this.metadata = metadata;
        this.arguments = arguments;
    }

    private ToolMetadata metadata;
    private Object[] arguments;

    public Method getMethod() {
        return metadata.method().getJavaMember();
    }

    public Object[] getArguments() {
        return arguments;
    }

    public Bean<?> getBean() {
        return metadata.bean();
    }
}
