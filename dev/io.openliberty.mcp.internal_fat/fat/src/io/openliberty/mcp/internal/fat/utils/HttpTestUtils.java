/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.utils;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;

/**
 *
 */
public class HttpTestUtils {

	public static String callMCP(LibertyServer server, String path, String jsonRequestBody) throws Exception {
		return new HttpRequest(server, path + "/mcp").requestProp("Accept", "application/json, text/event-stream")
				.jsonBody(jsonRequestBody).method("POST").run(String.class);
	};
};
