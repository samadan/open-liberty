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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.mcp.internal.requests.McpRequestId;

/**
 *
 */
public class McpResultResponse extends McpResponse {
    private static final TraceComponent tc = Tr.register(McpResultResponse.class);

    /**
     * @param id
     * @param result
     */
    private Object result;

    public McpResultResponse(McpRequestId id, Object result) {
        super("2.0", id);
        if (id.getStrVal() != null && id.getStrVal().isBlank())
            throw new IllegalArgumentException(Tr.formatMessage(tc, "CWMCM0024E.jsonrpc.validation.empty.string.id", id.getStrVal()));
        if (result == null)
            throw new IllegalArgumentException(Tr.formatMessage(tc, "CWMCM0031E.jsonrpc.missing.result"));
        this.result = result;
    }

    public Object getResult() {
        return result;
    }

}
