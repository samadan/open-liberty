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

import static componenttest.annotation.SkipIfSysProp.DB_DB2;
import static componenttest.annotation.SkipIfSysProp.DB_Not_Default;
import static componenttest.annotation.SkipIfSysProp.DB_Oracle;
import static componenttest.annotation.SkipIfSysProp.DB_Postgres;
import static componenttest.annotation.SkipIfSysProp.DB_SQLServer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.annotation.OnlyIfSysProp;
import componenttest.annotation.SkipIfSysProp;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class Data_1_1_Servlet extends FATServlet {

    @Override
    public void init(ServletConfig config) throws ServletException {
        // Some read-only data that is prepopulated for tests:
        // TODO
    }

    /**
     * TODO replace this with a useful test
     */
    @Test
    public void testData_1_1() {
        System.out.println("testData_1_1 running");
    }
}
