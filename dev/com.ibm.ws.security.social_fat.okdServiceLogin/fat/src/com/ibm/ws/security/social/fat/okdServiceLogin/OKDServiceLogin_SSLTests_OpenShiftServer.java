/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.fat.okdServiceLogin;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.social.fat.okdServiceLogin.RepeatActions.OKDServiceLoginRepeatActions;
import com.ibm.ws.security.social.fat.okdServiceLogin.commonTests.OKDServiceLogin_SSLTests;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialMessageConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * SSL targeted testing of OKD Service Account using a real OpenShift user validation endpoint
 **/

@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class OKDServiceLogin_SSLTests_OpenShiftServer extends OKDServiceLogin_SSLTests {

    public static Class<?> thisClass = OKDServiceLogin_SSLTests_OpenShiftServer.class;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(OKDServiceLoginRepeatActions.usingOpenShift());

    @BeforeClass
    public static void setUp() throws Exception {

        stubbedTests = false;

        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKT0016I.*" + SocialConstants.SOCIAL_DEFAULT_CONTEXT_ROOT);

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SocialConstants.HELLOWORLD_SERVLET);

        socialSettings = new SocialTestSettings();
        testSettings = socialSettings;

        genericTestServer = commonSetUp(SocialConstants.SERVER_NAME + ".OKDServiceLogin.social", "server_OKDServiceLogin_SSLTests_usingOpenShift.xml", SocialConstants.GENERIC_SERVER, extraApps, SocialConstants.DO_NOT_USE_DERBY, startMsgs);

        setActionsForProvider(SocialConstants.OPENSHIFT_PROVIDER, SocialConstants.OAUTH_OP);

        setGenericVSSpeicificProviderFlags(GenericConfig, "server_OKDServiceLogin_SSLTests_usingOpenShift");

        socialSettings = updateOkdServiceLoginSettings(socialSettings, genericTestServer);

        serviceAccountToken = genericTestServer.getBootstrapProperty("service.account.token");
        Log.info(thisClass, "setup", "service.account.token: " + serviceAccountToken);

        genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKG0033W_ATTRIBUTE_VALUE_NOT_FOUND);

    }

}
