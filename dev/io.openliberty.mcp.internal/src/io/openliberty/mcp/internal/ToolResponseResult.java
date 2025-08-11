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
import java.util.List;

/**
 *
 */
public class ToolResponseResult {

    public ToolResponseResult(Object result) {
        content.add(new TextContent(result));
    }

    public List<Content> getContent() {
        return content;
    }

    private List<Content> content = new ArrayList<>();

    public static abstract class Content {
        private final String type;

        public Content(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public static class TextContent extends Content {

        public Object getText() {
            return text;
        }

        private final Object text;

        public TextContent(Object text) {
            super("text");
            this.text = text;
        }
    }
}
