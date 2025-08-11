/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.basicToolApp;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;

/**
 *
 */
@ApplicationScoped
public class BasicTools {

    @Tool(name = "echo", title = "Echoes the input", description = "Returns the input unchanged")
    public String echo(@ToolArg(name = "input", description = "input to echo") String input) {
        if (input.equals("throw error")) {
            throw new RuntimeException("Method call caused runtime exception");
        }
        return input;
    }

    @Tool(name = "privateEcho", title = "Echoes the input", description = "Returns the input unchanged")
    private String privateEcho(@ToolArg(name = "input", description = "input to echo") String input) {
        return input;
    }

    @Tool(name = "add", title = "Addition calculator", description = "Returns the sum of the two inputs")
    public int add(@ToolArg(name = "num1", description = "first number") int number1, @ToolArg(name = "num2", description = "second number") int number2) {
        return number1 + number2;
    }
}
