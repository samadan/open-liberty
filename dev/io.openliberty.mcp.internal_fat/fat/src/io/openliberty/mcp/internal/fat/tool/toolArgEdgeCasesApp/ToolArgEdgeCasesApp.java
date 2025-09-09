/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.toolArgEdgeCasesApp;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;

/**
 *
 */
@ApplicationScoped
public class ToolArgEdgeCasesApp {

    // duplicate parameter names
    @Tool(name = "duplicateParam")
    public String duplicateParam(@ToolArg(name = "arg") String arg, @ToolArg(name = "arg") String notArg) {
        return notArg;
    }

    // duplicate parameter names variant
    @Tool(name = "duplicateParamVariant")
    public String duplicateParamVariant(@ToolArg(name = "arg") String arg, @ToolArg(name = "arg") String notArg, @ToolArg(name = "arg") String notArgAgain) {
        return notArgAgain;
    }

    // arg name value is blank
    @Tool(name = "argNameisBlank")
    public String argNameisBlank(@ToolArg(name = "") String arg1) {
        return arg1;
    }

    // arg name value is blank
    @Tool(name = "argNameisBlankVariant")
    public String argNameisBlankVariant(@ToolArg(name = "") String arg1, @ToolArg(name = "") String arg2) {
        return arg1;
    }

    // special characters in name
    @Tool(name = "specialCharactersInName")
    public String specialCharactersInName(@ToolArg(name = "@arg1") String arg1, @ToolArg(name = "@arg2") String arg2) {
        return arg1;
    }
}
