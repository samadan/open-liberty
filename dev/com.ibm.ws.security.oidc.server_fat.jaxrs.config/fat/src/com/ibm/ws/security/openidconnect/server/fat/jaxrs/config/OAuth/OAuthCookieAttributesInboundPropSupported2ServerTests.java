/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OAuth;

import org.junit.Assume;
import org.junit.BeforeClass;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.CommonTests.CookieAttributes2ServerTests;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;
import componenttest.topology.utils.LDAPUtils;

@LibertyServerWrapper
@Mode(TestMode.FULL)
public class OAuthCookieAttributesInboundPropSupported2ServerTests extends CookieAttributes2ServerTests {

    private static final Class<?> thisClass = OAuthCookieAttributesInboundPropSupported2ServerTests.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {
    	/*
    	 * These tests have not been configured to run with the local LDAP server.
    	 */
    	Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        tokenTestingSetup("server_disableLtpaCookie_authnSessionDisabled_supported.xml", Constants.SUPPORTED, Constants.OAUTH_OP, false);
        testSettings.setInboundProp(Constants.SUPPORTED);
    }

}
