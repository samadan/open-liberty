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
package test.jakarta.data.v1_1.web;

import java.util.Collection;
import java.util.stream.Stream;

import jakarta.data.Order;
import jakarta.data.constraint.AtLeast;
import jakarta.data.constraint.AtMost;
import jakarta.data.constraint.EqualTo;
import jakarta.data.constraint.NotEqualTo;
import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Is;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;

/**
 * Repository for the Fraction entity
 */
@Repository
public interface Fractions {
    @Find
    @OrderBy(_Fraction.NUMERATOR)
    @OrderBy(_Fraction.DENOMINATOR)
    Stream<Fraction> havingDenoninatorWithin//
    (@By(_Fraction.DENOMINATOR) @Is(AtLeast.class) long min,
     @By(_Fraction.DENOMINATOR) @Is(AtMost.class) long max);

    @Insert
    void supply(Collection<Fraction> list);

    @Find
    Stream<Fraction> withDenominatorButNotNumerator //
    (@By(_Fraction.DENOMINATOR) @Is(EqualTo.class) long denominator,
     @By(_Fraction.NUMERATOR) @Is(NotEqualTo.class) long excludeNumerator,
     Order<Fraction> order);
}
