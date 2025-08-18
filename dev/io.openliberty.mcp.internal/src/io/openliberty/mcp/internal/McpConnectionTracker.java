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

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.openliberty.mcp.messaging.Cancellation;
import jakarta.inject.Singleton;

/**
 * The connection from an MCP client.
 */

@Singleton
public class McpConnectionTracker {

    private final ConcurrentMap<String, Optional<String>> cancellationRequests;
    private final ConcurrentMap<String, Cancellation> ongoingRequests;

    /**
     * @param cancellationRequests
     */
    public McpConnectionTracker() {
        this.cancellationRequests = new ConcurrentHashMap<>();
        this.ongoingRequests = new ConcurrentHashMap<>();
    }

    /**
     * @return the cancellationRequests
     */
    public Optional<String> getCancellationReason(String requestId) {
        return cancellationRequests.get(requestId);
    }

    /**
     * @param cancellationRequests the cancellationRequests to set
     */
    public void addCancellationRequest(String requestId, Optional<String> reason) {
        cancellationRequests.putIfAbsent(requestId, reason);
    }

    public void removeCancellationRequest(String requestId) {
        if (!cancellationRequests.isEmpty()) {
            cancellationRequests.remove(requestId);
        }
    }

    /**
     * @return the ongoingRequests
     */
    public void deregisterOngoingRequest(String id) {
        ongoingRequests.remove(id);
    }

    public void registerOngoingRequest(String id, Cancellation cancellation) {
        ongoingRequests.putIfAbsent(id, cancellation);
    }

    public void updateOngoingProcess(String id, Cancellation cancellation) {
        ongoingRequests.put(id, cancellation);
    }

    public boolean isOngoingProcess(String id) {
        return ongoingRequests.containsKey(id);
    }

}
