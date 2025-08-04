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
public class ToolResponse {

    private final String jsonrpc = "2.0";
    private final Object id;
    private final Result result;

    public static ToolResponse createFor(Object id, Object result) {
        if (result instanceof String s) {
            ToolResponse response = new ToolResponse(id);
            response.result.content.add(new TextContent(s));
            return response;
        } else {
            throw new RuntimeException("TODO: handle non-string responses");
        }
    }

    public ToolResponse(Object id) {
        this.id = id;
        result = new Result();
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public Object getId() {
        return id;
    }

    public Result getResult() {
        return result;
    }

    public static class Result {
        public List<Content> getContent() {
            return content;
        }

        private List<Content> content = new ArrayList<>();
    }

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