/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.responses;

import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

/**
 *
 */
public class McpErrorResponse extends McpResponse {

    /**
     * @param jsonrpc
     * @param id
     */
    Error error;

    public McpErrorResponse(Object id, JSONRPCException e) {
        super("2.0", id);
        try {
            this.error = new Error(e.getErrorCode().getCode(), e.getErrorCode().getMessage(), e.getData());
        } catch (Exception ex) {
            throw new RuntimeException("Could not create error object");
        }

    }

    public McpErrorResponse(Object id, Error e) {
        super("2.0", id);
        try {
            this.error = e;
        } catch (Exception ex) {
            throw new RuntimeException("Could not create error object");
        }

    }

    public Error getError() {
        return error;
    }

    public static class Error {
        private int code;
        private String message;
        private Object data;

        @JsonbCreator
        Error(@JsonbProperty("code") int code, @JsonbProperty("message") String message, @JsonbProperty("code") Object data) {
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

}
