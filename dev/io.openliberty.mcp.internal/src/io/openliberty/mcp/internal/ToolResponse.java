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

import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

/**
 *
 */
public class ToolResponse {

    private final String jsonrpc = "2.0";
    private final Object id;
    private Result result;
    private Error error;

    public static ToolResponse createSuccess(Object id, String output) {
        ToolResponse response = new ToolResponse(id);
        response.result = new Result();
        response.result.content.add(new TextContent(output));
        return response;
    }

    public static ToolResponse createError(Object id, JSONRPCException e) {
        ToolResponse response = new ToolResponse(id);
        response.error = new Error(e.getErrorCode().getCode(), e.getErrorCode().getMessage(), e.getData());
        return response;
    }

    public ToolResponse(Object id) {
        this.id = id;
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

    public Error getError() {
        return error;
    }

    public static class Result {
        public List<Content> getContent() {
            return content;
        }

        private List<Content> content = new ArrayList<>();
    }

    public static class Error {
        private int code;
        private String message;
        private Object data;

        @JsonbCreator
        private Error(@JsonbProperty("code") int code, @JsonbProperty("message") String message, @JsonbProperty("code") Object data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }

        /**
         * @return the code
         */
        public int getCode() {
            return code;
        }

        /**
         * @return the message
         */
        public String getMessage() {
            return message;
        }

        /**
         * @return the data
         */
        public Object getData() {
            return data;
        }

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