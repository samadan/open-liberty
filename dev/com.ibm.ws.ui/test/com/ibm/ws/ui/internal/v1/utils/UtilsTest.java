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
package com.ibm.ws.ui.internal.v1.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.MalformedURLException;

import org.junit.Test;

/**
 * 
 */
public class UtilsTest {

    @Test
    public void urlEncoded() {
        assertEquals("Drives Utils.urlEncoded for happy path coverage... this test is here because we care about coverage and no other real reason",
                     "encodeMe", Utils.urlEncode("encodeMe"));
    }

    @Test
    public void urlEncoded_null() {
        assertNull("Drives Utils.urlEncoded for happy path coverage...",
                   Utils.urlEncode(null));
    }

    @Test
    public void getURL() throws Exception {
        assertNotNull("Drives Utils.getURL for happy path coverage... this test is here because we care about coverage and no other real reason",
                      Utils.getURL("http://www.ibm.com"));
    }

    @Test(expected = MalformedURLException.class)
    public void getURL_badURL() throws Exception {
        Utils.getURL("tp:/.ibm.com");
    }

    @Test
    public void md5Test() {
        assertEquals("MD5 encoding not working", "9EC5C2C0F355B0B1EA8319C9FCD6E44F".toLowerCase(), Utils.getMD5String("Test MD5 String"));
    }
}
