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

import java.lang.reflect.Type;

import io.openliberty.mcp.messaging.Cancellation;

public enum SpecialArgumentType {
    CANCELLATION(Cancellation.class);

    private final Class<?> typeClass;

    SpecialArgumentType(Class<?> typeClass) {
        this.typeClass = typeClass;
    }

    public static SpecialArgumentType fromClass(Type type) {
        for (SpecialArgumentType specialArgType : values()) {
            if (specialArgType.typeClass.equals(type)) {
                return specialArgType;
            }
        }
        throw new IllegalArgumentException();
    }
}
