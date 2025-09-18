/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.noparamtool.basicToolApp;

import java.util.List;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.Tool.Annotations;
import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.content.AudioContent;
import io.openliberty.mcp.content.Content;
import io.openliberty.mcp.content.ImageContent;
import io.openliberty.mcp.content.TextContent;
import io.openliberty.mcp.tools.ToolResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;

/**
 *
 */
@ApplicationScoped
public class BasicTools {
    
    @Tool(name = "illegalToolArgNameTool", title = "illegal ToolArgName Tool", description = "thows illegalArgumentExeption because of the ToolArgName")
    public String illegalToolArgNameTool(@ToolArg(name = Tool.ELEMENT_NAME, description = "input to echo") String input) {
        return input;
    }
}