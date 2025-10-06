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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.internal.ToolMetadata.ArgumentMetadata;
import io.openliberty.mcp.internal.ToolMetadata.SpecialArgumentMetadata;
import io.openliberty.mcp.internal.schemas.SchemaRegistry;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessManagedBean;

/**
 * Finds tools
 */

public class McpCdiExtension implements Extension {

    private static final TraceComponent tc = Tr.register(McpCdiExtension.class);

    private ToolRegistry tools = new ToolRegistry();
    private ConcurrentHashMap<String, LinkedList<String>> duplicateToolsMap = new ConcurrentHashMap<>();

    private SchemaRegistry schemas = new SchemaRegistry();

    void registerTools(@Observes ProcessManagedBean<?> pmb) {
        AnnotatedType<?> type = pmb.getAnnotatedBeanClass();
        Class<?> javaClass = type.getJavaClass();
        for (AnnotatedMethod<?> m : type.getMethods()) {
            Tool toolAnnotation = m.getAnnotation(Tool.class);
            if (toolAnnotation != null) {
                registerTool(toolAnnotation, pmb.getBean(), m);
            }
        }
    }

    void afterDeploymentValidation(@Observes AfterDeploymentValidation afterDeploymentValidation, BeanManager manager) {
        reportOnDuplicateTools(afterDeploymentValidation);
        reportOnToolArgEdgeCases(afterDeploymentValidation);
        reportOnDuplicateSpecialArguments(afterDeploymentValidation);
        reportOnInvalidSpecialArguments(afterDeploymentValidation);
    }

    /**
     * @param afterDeploymentValidation
     */
    private void reportOnToolArgEdgeCases(AfterDeploymentValidation afterDeploymentValidation) {
        List<String> blankArgsList = new ArrayList<>();
        List<String> duplicateArgsList = new ArrayList<>();
        List<String> missingArgsList = new ArrayList<>();
        boolean blankArgumentsFound = false;
        boolean duplicateArgumentsFound = false;
        boolean missingArgumentName = false;

        for (ToolMetadata tool : tools.getAllTools()) {
            Map<String, ArgumentMetadata> arguments = tool.arguments();

            for (String argName : arguments.keySet()) {
                if (argName.isBlank()) {
                    blankArgsList.add(tool.getToolQualifiedName());
                    blankArgumentsFound = true;
                } else if (arguments.get(argName).isDuplicate()) {
                    duplicateArgsList.add(tool.getToolQualifiedName() + " -  Argument: " + argName);
                    duplicateArgumentsFound = true;
                } else if (argName.equals(ToolMetadata.MISSING_TOOL_ARG_NAME)) {
                    missingArgsList.add(tool.getToolQualifiedName());
                    missingArgumentName = true;
                }
            }
        }
        if (blankArgumentsFound) {
            afterDeploymentValidation.addDeploymentProblem(new Exception(Tr.formatMessage(tc, "CWMCM0001E.blank.arguments", String.join(",\n", blankArgsList))));

        }
        if (duplicateArgumentsFound) {
            afterDeploymentValidation.addDeploymentProblem(new Exception(Tr.formatMessage(tc, "CWMCM0002E.duplicate.arguments", String.join(",\n", duplicateArgsList))));
        }
        if (missingArgumentName) {
            afterDeploymentValidation.addDeploymentProblem(new Exception(Tr.formatMessage(tc, "CWMCM0003E.missing.tool.argument.name", String.join(",\n", missingArgsList))));
        }
    }

    private void reportOnDuplicateTools(AfterDeploymentValidation afterDeploymentValidation) {
        // prune items that are not duplicates
        duplicateToolsMap.entrySet().removeIf(e -> e.getValue().size() == 1);
        List<String> duplicateToolsList = new ArrayList<>();
        for (String toolName : duplicateToolsMap.keySet()) {
            StringBuilder sb = new StringBuilder();
            LinkedList<String> qualifiedNames = duplicateToolsMap.get(toolName);
            sb.append("Tool: ").append(toolName);
            sb.append(" -- Methods found:\n");
            for (String qualifiedName : qualifiedNames) {
                sb.append("    - ").append(qualifiedName + "\n");
            }
            duplicateToolsList.add(sb.toString());
        }

        if (duplicateToolsMap.size() != 0) {
            afterDeploymentValidation.addDeploymentProblem(new Exception(Tr.formatMessage(tc, "CWMCM0004E.duplicate.tools", String.join(",", duplicateToolsList))));
        }
    }

    private void reportOnDuplicateSpecialArguments(AfterDeploymentValidation afterDeploymentValidation) {
        for (ToolMetadata tool : tools.getAllTools()) {
            Map<SpecialArgumentType.Resolution, Integer> resultCountMap = new HashMap<>();
            for (SpecialArgumentMetadata specialArgument : tool.specialArguments()) {
                SpecialArgumentType.Resolution specialArgumentTypeResolution = specialArgument.typeResolution();
                if (specialArgumentTypeResolution.specialArgsType() == SpecialArgumentType.UNSUPPORTED) {
                    continue;
                }
                resultCountMap.merge(specialArgumentTypeResolution, 1, Integer::sum);
//                if (resultCountMap.get(specialArgumentTypeResolution) > 1) {
//                    sbDuplicateSpecialArgs.append(specialArgumentTypeResolution);
//                    sbDuplicateSpecialArgs.append(" Tool: " + tool.getToolQualifiedName());
//                    duplicateArgsList.add(sbDuplicateSpecialArgs.toString());
//                    afterDeploymentValidation.addDeploymentProblem(new Exception(Tr.formatMessage(tc, "CWMCM0005E.duplicate.special.arguments",
//                                                                                                  String.join(",", duplicateArgsList))));
//                }

            }
            resultCountMap.forEach((k, v) -> {
                if (v > 1) {
                    afterDeploymentValidation.addDeploymentProblem(new Exception(Tr.formatMessage(tc, "CWMCM0005E.duplicate.special.arguments", tool.getToolQualifiedName(), k)));
                }
            });

        }
    }

    private void reportOnInvalidSpecialArguments(AfterDeploymentValidation afterDeploymentValidation) {
        List<String> invalidArgsList = new ArrayList<>();
        for (ToolMetadata tool : tools.getAllTools()) {
            for (SpecialArgumentMetadata specialArgument : tool.specialArguments()) {
                if (specialArgument.typeResolution().specialArgsType() == SpecialArgumentType.UNSUPPORTED) {
                    afterDeploymentValidation.addDeploymentProblem(new Exception(Tr.formatMessage(tc, "CWMCM0006E.invalid.arguments", tool.getToolQualifiedName(),
                                                                                                  specialArgument.typeResolution())));
                }
            }
        }
    }

    private void registerTool(Tool tool, Bean<?> bean, AnnotatedMethod<?> method) {
        ToolMetadata toolmd = ToolMetadata.createFrom(tool, bean, method);
        duplicateToolsMap.computeIfAbsent(toolmd.name(), key -> new LinkedList<>()).add(toolmd.getToolQualifiedName());
        tools.addTool(toolmd);
        if (TraceComponent.isAnyTracingEnabled()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Registered tool: " + toolmd.name(), toolmd);
            } else if (tc.isEventEnabled()) {
                Tr.event(this, tc, "Registered tool: " + toolmd.name(), method);
            }
        }
    }

    public ToolRegistry getToolRegistry() {
        return tools;
    }

    public SchemaRegistry getSchemaRegistry() {
        return schemas;
    }

}
