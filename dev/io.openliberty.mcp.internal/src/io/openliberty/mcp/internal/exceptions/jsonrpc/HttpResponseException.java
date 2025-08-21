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

import java.util.Map;

/**
 * Custom exception class for HTTP related errors.
 * Used to handle exceptions where the HTTP request is in the incorrect format for the MCP Server to handle
 */
public class HttpResponseException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private int statusCode;
    private Map<String, String> header;
    private String contentType;

    public HttpResponseException(int statusCode, String msg, String contentType) {
        super(msg);
        this.statusCode = statusCode;
        this.contentType = contentType;
    }

    /**
     * @return the header
     */
    public Map<String, String> getHeader() {
        return header;
    }

    /**
     * @param header the header to set
     */
    public void setHeader(Map<String, String> header) {
        this.header = header;
    }

    /**
     * @return the contentType
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns the HTTP status code associated with this exception.
     *
     * @return the HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }

}
