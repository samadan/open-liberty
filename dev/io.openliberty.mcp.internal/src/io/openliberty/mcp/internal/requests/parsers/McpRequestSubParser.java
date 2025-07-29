/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.requests.parsers;

import io.openliberty.mcp.internal.RequestMethod;
import io.openliberty.mcp.internal.requests.McpRequest;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.stream.JsonParser;

/**
 *
 */
public interface McpRequestSubParser<T extends McpRequest> {

    public T parse(JsonParser parser, String id, RequestMethod method, DeserializationContext ctx);

}
