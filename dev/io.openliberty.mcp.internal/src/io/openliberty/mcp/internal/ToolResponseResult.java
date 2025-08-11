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
    private boolean isError = false;

    public ToolResponseResult(Object result, boolean isError) {
        this.isError = isError;

        if (result instanceof String s) {
            content.add(new TextContent(s));
        } else {
            throw new RuntimeException("TODO: handle non-string responses");
        }
    }

    public List<Content> getContent() {
        return content;
    }

    /**
     * @return the isError
     */
    public boolean getIsError() {
        return isError;
    }

    private List<Content> content = new ArrayList<>();

    public static abstract class Content {
        private String type;

        public Content(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public static class TextContent extends Content {

        public String getText() {
            return text;
        }

        private String text;

        public TextContent(String text) {
            super("text");
            this.text = text;
        }
    }
}
