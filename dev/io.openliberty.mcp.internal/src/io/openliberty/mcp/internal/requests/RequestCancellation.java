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

import java.util.Optional;

import io.openliberty.mcp.messaging.Cancellation;

/**
 *
 */
public class RequestCancellation implements Cancellation {

    private String requestId;
    private volatile Optional<String> reason;

    public RequestCancellation() {}

    /**
     *
     */
    public RequestCancellation(String requestId, Optional<String> reason) {
        this.requestId = requestId;
        this.reason = reason;
    }

    /** {@inheritDoc} */
    @Override
    public Result check() {
        if (requestId == null) {
            return new Result(false, Optional.empty());
        }
        if (reason == null || reason.isEmpty()) {
            return new Result(false, Optional.empty());
        }
        return new Result(true, reason);
    }

    /**
     * @param requestId the requestId to set
     */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
     * @param reason the reason to set
     */
    public void setReason(Optional<String> reason) {
        this.reason = reason;
    }

}
