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
import jakarta.enterprise.util.AnnotationLiteral;

/**
 *
 */
public class Literals {

    private static abstract class ToolLiteral extends AnnotationLiteral<Tool> implements Tool {
        private static final long serialVersionUID = 1L;
    }

    public static Tool tool(String name, String title, String description) {
        return new ToolLiteral() {
            private static final long serialVersionUID = 1L;

            @Override
            public String title() {
                return title;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return description;
            }
        };
    }

}
