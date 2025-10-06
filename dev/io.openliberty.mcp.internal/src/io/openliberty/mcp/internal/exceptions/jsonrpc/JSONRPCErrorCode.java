/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.exceptions.jsonrpc;

/**
 * Enum to map standard JSONRPC errors
 *
 * @param code
 * @param message
 */
public enum JSONRPCErrorCode {
    PARSE_ERROR(-32700, "CWMCM0009E.jsonrpc.parse.error"),
    INVALID_REQUEST(-32600, "CWMCM0010E.jsonrpc.invalid.request"),
    METHOD_NOT_FOUND(-32601, "CWMCM0011E.jsonrpc.unkown.method"),
    INVALID_PARAMS(-32602, "CWMCM0012E.jsonrpc.invalid.params"),
    INTERNAL_ERROR(-32603, "CWMCM0013E.jsonrpc.internal.error");

    private int code;
    private String message;

    private JSONRPCErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
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

}
