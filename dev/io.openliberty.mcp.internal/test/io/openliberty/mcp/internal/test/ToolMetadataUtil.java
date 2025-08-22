/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test;

import java.util.Map;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.internal.ToolMetadata;
import io.openliberty.mcp.internal.ToolMetadata.ArgumentMetadata;

/**
 *
 */
public class ToolMetadataUtil {

    public static ToolMetadata createToolMetadataFrom(Tool annotation, Map<String, ArgumentMetadata> arguments) {

        String title = annotation.title();
        if (title.equals("")) {
            title = null;
        }

        return new ToolMetadata(annotation.annotations(), annotation, null, null, arguments, annotation.name(), annotation.description(), title);
    }
}
