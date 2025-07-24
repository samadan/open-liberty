/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jakarta.data.metamodel;

import jakarta.data.expression.NumericExpression;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface NumericAttribute<T, N extends Number & Comparable<N>> //
                extends ComparableAttribute<T, N>, NumericExpression<T, N> {

    static <T, N extends Number & Comparable<N>> NumericAttribute<T, N> //
                    of(Class<T> entityClass, String name, Class<N> attributeType) {

        if (entityClass == null ||
            name == null ||
            attributeType == null)
            throw new NullPointerException();

        return new NumericAttributeRecord<>(entityClass, name, attributeType);
    }
}
