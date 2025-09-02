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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.openliberty.mcp.internal.requests.RequestId;
import io.openliberty.mcp.messaging.Cancellation;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * This is a connection tracker bean. It keeps track of ongoing tool call requests
 */

@ApplicationScoped
public class McpConnectionTracker {

    private final ConcurrentMap<String, Cancellation> ongoingRequests;

    public McpConnectionTracker() {
        this.ongoingRequests = new ConcurrentHashMap<>();
    }

    public void deregisterOngoingRequest(RequestId id) {
        ongoingRequests.remove(id.getUniqueId());
    }

    public void registerOngoingRequest(RequestId id, Cancellation cancellation) {
        ongoingRequests.putIfAbsent(id.getUniqueId(), cancellation);
    }

    public boolean isOngoingRequest(RequestId id) {
        return ongoingRequests.containsKey(id.getUniqueId());
    }

    public Cancellation getOngoingRequestCancelation(RequestId id) {
        if (isOngoingRequest(id)) {
            return ongoingRequests.get(id.getUniqueId());
        }
        return null;
    }
}
