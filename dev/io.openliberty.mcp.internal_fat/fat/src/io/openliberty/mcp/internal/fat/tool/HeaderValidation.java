/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool;

/**
 *
 */
public class HeaderValidation {
    public static boolean acceptContains(String acceptHeader, String mime) {
        if (acceptHeader == null)
            return false;
        String target = mime.toLowerCase(java.util.Locale.ROOT);
        for (String part : acceptHeader.split(",")) {
            String value = part.trim().toLowerCase(java.util.Locale.ROOT);
            if (value.startsWith(target))
                return true;
        }
        return false;
    }

}
