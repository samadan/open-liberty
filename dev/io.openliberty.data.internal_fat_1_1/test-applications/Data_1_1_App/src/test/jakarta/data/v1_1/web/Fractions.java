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
    @OrderBy("numerator")
    @OrderBy("denominator")
    Stream<Fraction> havingDenoninatorWithin//
    (@By("denominator") @Is(AtLeast.class) long min,
     @By("denominator") @Is(AtMost.class) long max);

    @Find
    Stream<Fraction> withDenominatorButNotNumerator //
    (@By("denominator") @Is(EqualTo.class) long denominator,
     @By("numerator") @Is(NotEqualTo.class) long excludeNumerator,
     Order<Fraction> order);

    @Insert
    void supply(Collection<Fraction> list);
}
