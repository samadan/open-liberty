/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.requests;

import io.openliberty.mcp.internal.RequestMethod;
import io.openliberty.mcp.internal.requests.parsers.McpRequestParser;
import jakarta.json.bind.annotation.JsonbTypeDeserializer;

@JsonbTypeDeserializer(McpRequestParser.class)
public class McpRequest {

    private String id;
    private RequestMethod requestMethod;

    /**
     * @param id
     * @param requestMethod
     */
    public McpRequest(String id, RequestMethod requestMethod) {
        super();
        this.id = id;
        this.requestMethod = requestMethod;
    }

    public String getId() {
        return id;
    }

    public RequestMethod getRequestMethod() {
        return requestMethod;
    }

}
