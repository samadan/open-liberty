/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test;

import java.util.List;
import java.util.Map;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.internal.ToolMetadata;

/**
 *
 */
public class ToolMetadataTestUtility {

    public static ToolMetadata createFrom(Tool annotation, Map<String, ToolMetadata.ArgumentMetadata> arguments, List<ToolMetadata.SpecialArgumentMetadata> specialArguments) {
        // used for unit Tests that pre-populate argumentData and create Tools within the tests
        String title = annotation.title().isEmpty() ? null : annotation.title();
        String qualifiedName = ""; // for unit tests we do not output qualified names as these are for the CDI aftervalidation deploymentProblem events
        return new ToolMetadata(annotation, null, null, arguments, specialArguments, annotation.name(), title, annotation.description(), qualifiedName, null);
    }
}
