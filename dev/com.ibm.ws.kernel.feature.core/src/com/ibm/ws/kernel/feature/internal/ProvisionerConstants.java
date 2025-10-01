/*******************************************************************************
 * Copyright (c) 2009, 2025 IBM Corporation and others.
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
package com.ibm.ws.kernel.feature.internal;

public interface ProvisionerConstants {
    /**
     * Strings for trace and nls messages (for those classes w/in the bundle that
     * use Tr)
     */
    String NLS_PROPS = "com.ibm.ws.kernel.feature.internal.resources.ProvisionerMessages",
                    TR_GROUP = "featureManager";

    /** Location of feature files */
    String LIB_FEATURE_PATH = "lib/features/";

    String PHASE_APPLICATION = "APPLICATION";
    String PHASE_APPLICATION_LATE = PHASE_APPLICATION + "_LATE";
    String PHASE_APPLICATION_EARLY = PHASE_APPLICATION + "_EARLY";
    String PHASE_SERVICE = "SERVICE";
    String PHASE_SERVICE_LATE = PHASE_SERVICE + "_LATE";
    String PHASE_SERVICE_EARLY = PHASE_SERVICE + "_EARLY";
    String PHASE_CONTAINER = "CONTAINER";
    String PHASE_CONTAINER_LATE = PHASE_CONTAINER + "_LATE";
    String PHASE_CONTAINER_EARLY = PHASE_CONTAINER + "_EARLY";
}
