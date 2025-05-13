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
package com.ibm.ws.kernel.server.internal;

import static com.ibm.ws.kernel.server.internal.InspectCommand.Introspectors.INTROSPECTORS;
import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;

import com.ibm.wsspi.logging.Introspector;

/**
 * An 'inspect' command for Liberty's OSGi Console
 */
@Component(
           service = Object.class,
           configurationPolicy = IGNORE,
           property = {
                        "osgi.command.scope=kernel",
                        "osgi.command.function=inspect",
                        "service.vendor=IBM"
           })
public class InspectCommand {
    @Descriptor("Lists or invokes the known introspections available in this server. Run `introspect help' for more information.")
    public void inspect(String... args) {
        if (args.length == 0) {
            INTROSPECTORS.listAll();
        } else if ("help".equalsIgnoreCase(args[0])) {
            if (args.length == 1)
                displayUsage();
            else
                Stream.of(args).skip(1).forEach(INTROSPECTORS::findAndDescribe);
        } else {
            Stream.of(args).forEach(INTROSPECTORS::findAndRun);
        }
    }

    private void displayUsage() {
        System.out.println("usage: inspect");
        System.out.println("       Lists available introspections.");
        System.out.println();
        System.out.println("usage: inspect <introspection>");
        System.out.println("       Displays the output of the named introspection.");
        System.out.println();
        System.out.println("usage: inspect help");
        System.out.println("       Displays this usage message.");
        System.out.println();
        System.out.println("usage: inspect help <introspection>");
        System.out.println("       Displays the description of the named introspection.");
    }

    /**
     * Retrieves each introspector in turn, and runs actions on it.
     */
    enum Introspectors {
        INTROSPECTORS;

        /**
         * Like Consumer<Introspector> but it allows exceptions to be thrown
         */
        private interface IntrospectorAction {
            void actOn(Introspector i) throws Exception;
        }

        void listAll() {
            System.out.println("Available introspections: ");
            SortedSet<String> set = new TreeSet<>();
            forEach("retrieve introspector name", i -> set.add("\t" + i.getIntrospectorName()));
            set.forEach(System.out::println);
        }

        void findAndDescribe(String name) {
            forEach("describe introspector", this::describe, i -> Objects.equals(name, i.getIntrospectorName()));
        }

        void findAndRun(String name) {
            forEach("run introspector", this::run, i -> Objects.equals(name, i.getIntrospectorName()));
        }

        private void describe(Introspector ii) {
            System.out.println(ii.getIntrospectorName());
            System.out.println(ii.getIntrospectorName().replaceAll(".", "="));
            System.out.println(ii.getIntrospectorDescription());
            System.out.println();
            System.out.println();
        }

        private void run(Introspector i) throws Exception {
            try (PrintWriter pw = new PrintWriter(System.out)) {
                i.introspect(pw);
                pw.flush();
            }
        }

        @SafeVarargs
        private final void forEach(String desc, IntrospectorAction action, Predicate<Introspector>... filters) {
            final BundleContext ctx = FrameworkUtil.getBundle(Introspectors.class).getBundleContext();
            final Collection<ServiceReference<Introspector>> refs;
            try {
                refs = ctx.getServiceReferences(Introspector.class, null);
            } catch (InvalidSyntaxException e) {
                throw new RuntimeException("error: unable to retrieve introspector service references", e);
            }

            for (final ServiceReference<Introspector> rrr : refs) {
                try {
                    final Introspector svc = ctx.getService(rrr);
                    // Check the filters, and ignore non-matching introspectors
                    try {
                        if (Stream.of(filters).anyMatch(f -> !f.test(svc)))
                            continue;
                    } catch (Exception e) {
                        System.err.println("error: failed to match introspector of type " + svc.getClass());
                        e.printStackTrace();
                        continue;
                    }
                    // found a match! run action on introspector
                    try (AutoCloseable ungetter = () -> ctx.ungetService(rrr)) {
                        try {
                            action.actOn(svc);
                        } catch (Exception e) {
                            System.err.println("error: failed to " + desc + " for service of type " + svc.getClass());
                        }
                    } catch (Exception e) {
                        System.err.println("error: problem occurred while ungetting service of type " + svc.getClass());
                    }
                } catch (Exception e) {
                    System.err.println("error: unable to retrieve service of type " + rrr.getClass().getName());
                    e.printStackTrace();
                }
            }
        }
    }
}
