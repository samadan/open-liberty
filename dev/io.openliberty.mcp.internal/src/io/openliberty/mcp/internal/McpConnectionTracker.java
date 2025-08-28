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

import io.openliberty.mcp.messaging.Cancellation;
import jakarta.inject.Singleton;

/**
 * This is a connection tracker bean. It keeps track of ongoing tool call requests
 */

@Singleton
public class McpConnectionTracker {

    private final ConcurrentMap<String, Cancellation> ongoingRequests;

    public McpConnectionTracker() {
        this.ongoingRequests = new ConcurrentHashMap<>();
    }

    public void deregisterOngoingRequest(String id) {
        ongoingRequests.remove(id);
    }

    public void registerOngoingRequest(String id, Cancellation cancellation) {
        ongoingRequests.putIfAbsent(id, cancellation);
    }

    public void updateOngoingRequest(String id, Cancellation cancellation) {
        ongoingRequests.put(id, cancellation);
    }

    public boolean isOngoingRequest(String id) {
        return ongoingRequests.containsKey(id);
    }

    public Cancellation getOngoingRequestCancelation(String id) {
        if (isOngoingRequest(id)) {
            return ongoingRequests.get(id);
        }
        return null;
    }
}
