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

    //////////
    // Strings
    //////////
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

    @Tool(name = "testJSONCharacter", title = "testJSONCharacter", description = "testJSONCharacter")
    public String testJSONCharacter(@ToolArg(name = "c", description = "Character") Character c) {
        return c.toString();
    }

    @Tool(name = "testJSONcharacter", title = "testJSONcharacter", description = "testJSONcharacter")
    public String testJSONCharacter(@ToolArg(name = "c", description = "char") char c) {
        return new String(c + "");
    }

    /////////////
    // Primitives
    /////////////
    @Tool(name = "testJSONlong", title = "testJSONlong", description = "testJSONlong")
    public long testJSONlong(@ToolArg(name = "num1", description = "long") long number1) {
        return number1;
    }

    @Tool(name = "testJSONdouble", title = "testJSONdouble", description = "testJSONdouble")
    public double testJSONdouble(@ToolArg(name = "num1", description = "double") double number1) {
        return number1;
    }

    @Tool(name = "testJSONbyte", title = "testJSONbyte", description = "testJSONbyte")
    public byte testJSONbyte(@ToolArg(name = "num1", description = "byte") byte number1) {
        return number1;
    }

    @Tool(name = "testJSONfloat", title = "testJSONfloat", description = "testJSONfloat")
    public float testJSONfloat(@ToolArg(name = "num1", description = "float") float number1) {
        return number1;
    }

    @Tool(name = "testJSONshort", title = "testJSONshort", description = "testJSONshort")
    public short testJSONshort(@ToolArg(name = "num1", description = "short") short number1) {
        return number1;
    }

    ///////////
    // Wrappers
    ///////////
    @Tool(name = "testJSONLong", title = "testJSONLong", description = "testJSONLong")
    public Long testJSONLong(@ToolArg(name = "num1", description = "Long") Long number1) {
        return number1;
    }

    @Tool(name = "testJSONDouble", title = "testJSONDouble", description = "testJSONDouble")
    public Double testJSONDouble(@ToolArg(name = "num1", description = "Double") Double number1) {
        return number1;
    }

    @Tool(name = "testJSONByte", title = "testJSONByte", description = "testJSONByte")
    public Byte testJSONByte(@ToolArg(name = "num1", description = "Byte") Byte number1) {
        return number1;
    }

    @Tool(name = "testJSONFloat", title = "testJSONFloat", description = "testJSONFloat")
    public Float testJSONFloat(@ToolArg(name = "num1", description = "Float") Float number1) {
        return number1;
    }

    @Tool(name = "testJSONShort", title = "testJSONShort", description = "testJSONShort")
    public Short testJSONShort(@ToolArg(name = "num1", description = "Short") Short number1) {
        return number1;
    }

    //////////
    // Integer
    //////////
    @Tool(name = "testJSONInteger", title = "testJSONInteger", description = "testJSONInteger")
    public int testJSONInteger(@ToolArg(name = "num1", description = "Integer") Integer number1) {
        return number1;
    }

    @Tool(name = "add", title = "Addition calculator", description = "Returns the sum of the two inputs")
    public int add(@ToolArg(name = "num1", description = "first number") int number1, @ToolArg(name = "num2", description = "second number") int number2) {
        return number1 + number2;
    }

    @Tool(name = "subtract", title = "Subtraction calculator", description = "Minus number 2 from number 1")
    public int subtract(@ToolArg(name = "num1", description = "") int number1, @ToolArg(name = "num2", description = "") int number2) {
        return number1 - number2;
    }

    /////////
    //Boolean
    /////////
    @Tool(name = "testJSONBoolean", title = "testJSONBoolean", description = "testJSONBoolean")
    public boolean testJSONBoolean(@ToolArg(name = "b", description = "Boolean") Boolean b) {
        return b;
    }

    @Tool(name = "toggle", title = "Boolean toggle", description = "toggles the boolean input")
    public boolean toggle(@ToolArg(name = "value", description = "boolean value") boolean value) {
        return !value;
    }

}
