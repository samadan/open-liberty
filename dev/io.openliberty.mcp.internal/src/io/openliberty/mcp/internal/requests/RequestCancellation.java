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

import io.openliberty.mcp.internal.McpConnectionTracker;
import io.openliberty.mcp.messaging.Cancellation;
import jakarta.inject.Inject;

/**
 *
 */
public class RequestCancellation implements Cancellation {

    private final String requestId;
    private final Optional<String> reason;

    @Inject
    private McpConnectionTracker connection;

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
        Optional<String> reason = connection.getCancellationReason(requestId);
        if (reason == null || reason.isEmpty()) {
            return new Result(false, Optional.empty());
        }
        return new Result(true, reason);
    }

}
