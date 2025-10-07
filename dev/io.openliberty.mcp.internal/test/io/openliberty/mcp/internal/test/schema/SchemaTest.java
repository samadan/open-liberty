/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test.schema;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import io.openliberty.mcp.annotations.Schema;
import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.internal.ToolMetadata;
import io.openliberty.mcp.internal.schemas.SchemaDirection;
import io.openliberty.mcp.internal.schemas.SchemaRegistry;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;

/**
 *
 */
public class SchemaTest {
    private static SchemaRegistry registry;

    @Schema(description = "A person object contains address, company objects")
    public static record person(@JsonbProperty("fullname") String name, address address, company company) {};

    public static record address(int number, @Schema(description = "A street object to represent complex streets") street street, String postcode,
                                 @JsonbTransient String directions) {};

    @Schema("{\"properties\": {  \"streetName\": { \"type\": \"string\" }, \"roadType\": { \"type\": \"string\" } }, \"required\": [ \"streetName\" ], \"type\": \"object\"}")
    public static record street(String streetname, String roadtype) {}

    public static record company(String name, address address, @Schema(description = "A list of employees (person object)") List<person> employees,
                                 @Schema(value = "{\"properties\": {\"key\":{ \"type\": \"integer\" }, \"value\":{ \"$ref\": \"#/$defs/person\" }},\"required\": [ ], \"type\": \"object\"}") Map<String, person> employeeRegistry) {};

    public static record partialPerson(String name, Optional<address> address, partialCompany partialCompany) {}

    public static record partialCompany(Optional<String> name, Optional<address> address,
                                        @Schema(description = "A list of employees (person object)") Optional<List<partialPerson>> employees,
                                        Optional<Map<String, Optional<partialPerson>>> employeeRegistry) {}

    public static class SoftwareCompanyEntry {
        SoftwareCompanyEntry.person person;
        String companyName;

        public SoftwareCompanyEntry(SoftwareCompanyEntry.person person, String companyName) {}

        public static record person(SchemaTest.person person, int softwareid) {}

        /**
         * @return the person
         */
        public SoftwareCompanyEntry.person getPerson() {
            return person;
        }

        /**
         * @param person the person to set
         */
        public void setPerson(SoftwareCompanyEntry.person person) {
            this.person = person;
        }

        /**
         * @return the companyName
         */
        public String getCompanyName() {
            return companyName;
        }

        /**
         * @param companyName the companyName to set
         */
        public void setCompanyName(String companyName) {
            this.companyName = companyName;
        }

    };

    public static class ConstructionCompanyEntry {
        ConstructionCompanyEntry.person person;
        String companyName;

        public ConstructionCompanyEntry(ConstructionCompanyEntry.person person, String companyName) {}

        public static record person(SchemaTest.person person, String constructionid) {}

        /**
         * @return the person
         */
        public ConstructionCompanyEntry.person getPerson() {
            return person;
        }

        /**
         * @param person the person to set
         */
        public void setPerson(ConstructionCompanyEntry.person person) {
            this.person = person;
        }

        /**
         * @return the companyName
         */
        public String getCompanyName() {
            return companyName;
        }

        /**
         * @param companyName the companyName to set
         */
        public void setCompanyName(String companyName) {
            this.companyName = companyName;
        }

    };

    public static class PortfolioEntry {
        SoftwareCompanyEntry sce;
        ConstructionCompanyEntry cce;

        public PortfolioEntry(SoftwareCompanyEntry sce, ConstructionCompanyEntry cce) {}

        /**
         * @return the sce
         */
        public SoftwareCompanyEntry getSce() {
            return sce;
        }

        /**
         * @param sce the sce to set
         */
        public void setSce(SoftwareCompanyEntry sce) {
            this.sce = sce;
        }

        /**
         * @return the cce
         */
        public ConstructionCompanyEntry getCce() {
            return cce;
        }

        /**
         * @param cce the cce to set
         */
        public void setCce(ConstructionCompanyEntry cce) {
            this.cce = cce;
        }

    };

    @Tool(name = "checkPerson", title = "checks if person is in employee list", description = "Returns boolean")
    public boolean checkPerson(@ToolArg(name = "person", description = "Person object") person person, @ToolArg(name = "company", description = "Company object") company company) {
        return true;
    }

    @Tool(name = "addPersonToList", title = "adds person to employee list", description = "adds person to employee list, returns nothing")
    public @Schema(description = "Returns list of person object") List<person> addPersonToList(@ToolArg(name = "employeeList",
                                                                                                        description = "List of people") List<person> employeeList,
                                                                                               @ToolArg(name = "person", description = "Person object") person person) {
        employeeList.add(person);
        return employeeList;
        //comment
    }

    @BeforeClass
    public static void setup() {
        registry = new SchemaRegistry();
    }

    @Test
    public void testPersonSchema() {
        String response = registry.getSchema(person.class, SchemaDirection.INPUT).toString();
        String expectedResponseString = """
                            {
                            "$defs": {
                                "address": {
                                    "properties": {
                                        "number": {
                                            "type": "integer"
                                        },
                                        "street": {
                                            "description": "A street object to represent complex streets",
                                            "properties": {
                                                "streetName": {
                                                    "type": "string"
                                                },
                                                "roadType": {
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "streetName"
                                            ],
                                            "type": "object"
                                        },
                                        "postcode": {
                                            "type": "string"
                                        }
                                    },
                                    "required": [
                                        "number",
                                        "street",
                                        "postcode"
                                    ],
                                    "type": "object"
                                },
                                "person": {
                                    "description": "A person object contains address, company objects",
                                    "properties": {
                                        "address": {
                                            "$ref": "#/$defs/address"
                                        },
                                        "company": {
                                            "properties": {
                                                "address": {
                                                    "$ref": "#/$defs/address"
                                                },
                                                "name": {
                                                    "type": "string"
                                                },
                                                "employees": {
                                                    "description": "A list of employees (person object)",
                                                    "items": {
                                                        "$ref": "#/$defs/person"
                                                    },
                                                    "type": "array"
                                                },
                                                "employeeRegistry": {
                                                    "properties": {
                                                        "value": {
                                                            "$ref": "#/$defs/person"
                                                        },
                                                        "key": {
                                                            "type": "integer"
                                                        }
                                                    },
                                                    "required": [],
                                                    "type": "object"
                                                }
                                            },
                                            "required": [
                                                "name",
                                                "address",
                                                "employees",
                                                "employeeRegistry"
                                            ],
                                            "type": "object"
                                        },
                                        "fullname": {
                                            "type": "string"
                                        }
                                    },
                                    "required": [
                                        "fullname",
                                        "address",
                                        "company"
                                    ],
                                    "type": "object"
                                }
                            },
                            "$ref": "#/$defs/person"
                        }
                            """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    public record Widget(String name, int flangeCount) {};

    @Tool(description = "Updates a widget in the database")
    public Widget updateWidget(@ToolArg(name = "id", description = "The ID of the widget to update") long id,
                               @ToolArg(name = "widget", description = "The new widget data") Widget widget) {
        throw new UnsupportedOperationException();
    }

    @Test
    public void testToolInputSchema() throws NoSuchMethodException, SecurityException {
        ToolMetadata toolMetadata = findTool("updateWidget");

        String toolInputSchema = registry.getToolInputSchema(toolMetadata).toString();
        String expectedSchema = """
                        {
                        "type" : "object",
                        "properties" : {
                            "id" : {
                                "type": "integer",
                                "description": "The ID of the widget to update"
                            },
                            "widget" : {
                                "type": "object",
                                "description": "The new widget data",
                                "properties": {
                                    "name": {
                                        "type" : "string"
                                    },
                                    "flangeCount": {
                                        "type" : "integer"
                                    }
                                },
                                "required": [
                                    "name",
                                    "flangeCount"
                                ]
                            }
                        },
                        "required": [
                            "widget",
                            "id"
                        ]
                        }
                        """;
        JSONAssert.assertEquals(expectedSchema, toolInputSchema, true);
    }

    @Test
    public void testToolOutputSchema() throws NoSuchMethodException, SecurityException {
        ToolMetadata toolMetadata = findTool("updateWidget");

        String toolInputSchema = registry.getToolOuputSchema(toolMetadata).toString();
        String expectedSchema = """
                        {
                            "type": "object",
                            "properties": {
                                "name": {
                                    "type" : "string"
                                },
                                "flangeCount": {
                                    "type" : "integer"
                                }
                            },
                            "required": [
                                "name",
                                "flangeCount"
                            ]
                        }
                        """;
        JSONAssert.assertEquals(expectedSchema, toolInputSchema, true);
    }

    /**
     * Finds a tool method in the current class by name
     *
     * @param name the name of the tool
     * @return the tool metadata
     */
    private ToolMetadata findTool(String name) {
        List<ToolMetadata> tools = Arrays.stream(SchemaTest.class.getDeclaredMethods())
                                         .filter(m -> m.isAnnotationPresent(Tool.class))
                                         .map(m -> ToolMetadata.createFrom(m.getAnnotation(Tool.class), null, new MockAnnotatedMethod<>(m)))
                                         .filter(m -> m.name().equals(name))
                                         .collect(Collectors.toList());
        if (tools.size() != 1) {
            throw new RuntimeException("Found " + tools.size() + " tools with name " + name);
        }

        return tools.get(0);
    }

    public record CompositeWidget(String name, int flangeCount, List<CompositeWidget> subwidgets) {}

    @Tool(description = "combine two widgets to make a new widget")
    public CompositeWidget combineWidgets(@ToolArg(name = "widgetA", description = "the first widget") CompositeWidget widgetA,
                                          @ToolArg(name = "widgetB", description = "the second widget") CompositeWidget widgetB) {
        throw new UnsupportedOperationException();
    }

    @Test
    public void testToolInputRecursive() {
        ToolMetadata toolMetadata = findTool("combineWidgets");
        String toolInputSchema = registry.getToolInputSchema(toolMetadata).toString();

        String expectedSchema = """
                        {
                            "$defs": {
                                "CompositeWidget": {
                                    "type" : "object",
                                    "properties": {
                                        "name": {
                                            "type": "string"
                                        },
                                        "flangeCount": {
                                            "type": "integer"
                                        },
                                        "subwidgets": {
                                            "type": "array",
                                            "items": {
                                                "$ref": "#/$defs/CompositeWidget"
                                            }
                                        }
                                    },
                                    "required" : [
                                        "name",
                                        "flangeCount",
                                        "subwidgets"
                                    ]
                                }
                            },
                            "type": "object",
                            "properties": {
                                "widgetA": {
                                    "$ref": "#/$defs/CompositeWidget",
                                    "description": "the first widget"
                                },
                                "widgetB": {
                                    "$ref": "#/$defs/CompositeWidget",
                                    "description": "the second widget"
                                }
                            },
                            "required": [
                                "widgetB",
                                "widgetA"
                            ]
                        }
                        """;
        JSONAssert.assertEquals(expectedSchema, toolInputSchema, true);
    }

    @Test
    public void testToolOutputRecursive() {
        ToolMetadata toolMetadata = findTool("combineWidgets");
        String toolInputSchema = registry.getToolOuputSchema(toolMetadata).toString();
        String expectedSchema = """
                        {
                            "$defs": {
                                "CompositeWidget": {
                                    "type" : "object",
                                    "properties": {
                                        "name": {
                                            "type": "string"
                                        },
                                        "flangeCount": {
                                            "type": "integer"
                                        },
                                        "subwidgets": {
                                            "type": "array",
                                            "items": {
                                                "$ref": "#/$defs/CompositeWidget"
                                            }
                                        }
                                    },
                                    "required" : [
                                        "name",
                                        "flangeCount",
                                        "subwidgets"
                                    ]
                                }
                            },
                            "$ref": "#/$defs/CompositeWidget",
                        }
                        """;
        JSONAssert.assertEquals(expectedSchema, toolInputSchema, true);

    }

    @Test
    public void testPersonCheckToolSchema() throws NoSuchMethodException, SecurityException {
        ToolMetadata toolMetadata = findTool("checkPerson");
        String response = registry.getToolInputSchema(toolMetadata).toString();
        String expectedResponseString = """
                        {
                            "$defs": {
                                "address": {
                                    "properties": {
                                        "number": {
                                            "type": "integer"
                                        },
                                        "street": {
                                            "description": "A street object to represent complex streets",
                                            "properties": {
                                                "streetName": {
                                                    "type": "string"
                                                },
                                                "roadType": {
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "streetName"
                                            ],
                                            "type": "object"
                                        },
                                        "postcode": {
                                            "type": "string"
                                        }
                                    },
                                    "required": [
                                        "number",
                                        "street",
                                        "postcode"
                                    ],
                                    "type": "object"
                                },
                                "person": {
                                    "description": "A person object contains address, company objects",
                                    "properties": {
                                        "address": {
                                            "$ref": "#/$defs/address"
                                        },
                                        "company": {
                                            "$ref": "#/$defs/company"
                                        },
                                        "fullname": {
                                            "type": "string"
                                        }
                                    },
                                    "required": [
                                        "fullname",
                                        "address",
                                        "company"
                                    ],
                                    "type": "object"
                                },
                                "company": {
                                    "properties": {
                                        "address": {
                                            "$ref": "#/$defs/address"
                                        },
                                        "name": {
                                            "type": "string"
                                        },
                                        "employees": {
                                            "description": "A list of employees (person object)",
                                            "items": {
                                                "$ref": "#/$defs/person"
                                            },
                                            "type": "array"
                                        },
                                        "employeeRegistry": {
                                            "properties": {
                                                "value": {
                                                    "$ref": "#/$defs/person"
                                                },
                                                "key": {
                                                    "type": "integer"
                                                }
                                            },
                                            "required": [],
                                            "type": "object"
                                        }
                                    },
                                    "required": [
                                        "name",
                                        "address",
                                        "employees",
                                        "employeeRegistry"
                                    ],
                                    "type": "object"
                                }
                            },
                            "properties": {
                                "person": {
                                    "$ref": "#/$defs/person",
                                    "description": "Person object"
                                },
                                "company": {
                                    "$ref": "#/$defs/company",
                                    "description": "Company object"
                                }
                            },
                            "required": [
                                "person",
                                "company"
                            ],
                            "type": "object"
                        }
                                                    """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testAddressSchema() {
        String response = registry.getSchema(address.class, SchemaDirection.INPUT).toString();
        String expectedResponseString = """
                           {
                            "properties": {
                                "number": {
                                    "type": "integer"
                                },
                                "street": {
                                    "description": "A street object to represent complex streets",
                                    "properties": {
                                        "streetName": {
                                            "type": "string"
                                        },
                                        "roadType": {
                                            "type": "string"
                                        }
                                    },
                                    "required": [
                                        "streetName"
                                    ],
                                    "type": "object"
                                },
                                "postcode": {
                                    "type": "string"
                                }
                            },
                            "required": [
                                "number",
                                "street",
                                "postcode"
                            ],
                            "type": "object"
                        }
                            """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testStreetSchema() {
        String response = registry.getSchema(street.class, SchemaDirection.INPUT).toString();
        String expectedResponseString = """
                        {
                          "properties": {
                            "streetName": {
                              "type": "string"
                            },
                            "roadType": {
                              "type": "string"
                            }
                          },
                          "required": [
                            "streetName"
                          ],
                          "type": "object"
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testCompanySchema() {
        String response = registry.getSchema(company.class, SchemaDirection.INPUT).toString();
        String expectedResponseString = """
                            {
                            "$defs": {
                                "address": {
                                    "properties": {
                                        "number": {
                                            "type": "integer"
                                        },
                                        "street": {
                                            "description": "A street object to represent complex streets",
                                            "properties": {
                                                "streetName": {
                                                    "type": "string"
                                                },
                                                "roadType": {
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "streetName"
                                            ],
                                            "type": "object"
                                        },
                                        "postcode": {
                                            "type": "string"
                                        }
                                    },
                                    "required": [
                                        "number",
                                        "street",
                                        "postcode"
                                    ],
                                    "type": "object"
                                },
                                "person": {
                                    "description": "A person object contains address, company objects",
                                    "properties": {
                                        "address": {
                                            "$ref": "#/$defs/address"
                                        },
                                        "company": {
                                            "$ref": "#/$defs/company"
                                        },
                                        "fullname": {
                                            "type": "string"
                                        }
                                    },
                                    "required": [
                                        "fullname",
                                        "address",
                                        "company"
                                    ],
                                    "type": "object"
                                },
                                "company": {
                                    "properties": {
                                        "address": {
                                            "$ref": "#/$defs/address"
                                        },
                                        "name": {
                                            "type": "string"
                                        },
                                        "employees": {
                                            "description": "A list of employees (person object)",
                                            "items": {
                                                "$ref": "#/$defs/person"
                                            },
                                            "type": "array"
                                        },
                                        "employeeRegistry": {
                                            "properties": {
                                                "value": {
                                                    "$ref": "#/$defs/person"
                                                },
                                                "key": {
                                                    "type": "integer"
                                                }
                                            },
                                            "required": [],
                                            "type": "object"
                                        }
                                    },
                                    "required": [
                                        "name",
                                        "address",
                                        "employees",
                                        "employeeRegistry"
                                    ],
                                    "type": "object"
                                }
                            },
                            "$ref": "#/$defs/company"
                        }
                                """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testPortfolioEntryDuplicateNameSchema() {
        String response = registry.getSchema(PortfolioEntry.class, SchemaDirection.INPUT).toString();
        String expectedResponseString = """
                            {
                            "$defs": {
                                "address": {
                                    "properties": {
                                        "number": {
                                            "type": "integer"
                                        },
                                        "street": {
                                            "description": "A street object to represent complex streets",
                                            "properties": {
                                                "streetName": {
                                                    "type": "string"
                                                },
                                                "roadType": {
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "streetName"
                                            ],
                                            "type": "object"
                                        },
                                        "postcode": {
                                            "type": "string"
                                        }
                                    },
                                    "required": [
                                        "number",
                                        "street",
                                        "postcode"
                                    ],
                                    "type": "object"
                                },
                                "person": {
                                    "description": "A person object contains address, company objects",
                                    "properties": {
                                        "address": {
                                            "$ref": "#/$defs/address"
                                        },
                                        "company": {
                                            "properties": {
                                                "address": {
                                                    "$ref": "#/$defs/address"
                                                },
                                                "name": {
                                                    "type": "string"
                                                },
                                                "employees": {
                                                    "description": "A list of employees (person object)",
                                                    "items": {
                                                        "$ref": "#/$defs/person"
                                                    },
                                                    "type": "array"
                                                },
                                                "employeeRegistry": {
                                                    "properties": {
                                                        "value": {
                                                            "$ref": "#/$defs/person"
                                                        },
                                                        "key": {
                                                            "type": "integer"
                                                        }
                                                    },
                                                    "required": [],
                                                    "type": "object"
                                                }
                                            },
                                            "required": [
                                                "name",
                                                "address",
                                                "employees",
                                                "employeeRegistry"
                                            ],
                                            "type": "object"
                                        },
                                        "fullname": {
                                            "type": "string"
                                        }
                                    },
                                    "required": [
                                        "fullname",
                                        "address",
                                        "company"
                                    ],
                                    "type": "object"
                                }
                            },
                            "properties": {
                                "sce": {
                                    "properties": {
                                        "person": {
                                            "properties": {
                                                "softwareid": {
                                                    "type": "integer"
                                                },
                                                "person": {
                                                    "$ref": "#/$defs/person"
                                                }
                                            },
                                            "required": [
                                                "person",
                                                "softwareid"
                                            ],
                                            "type": "object"
                                        },
                                        "companyName": {
                                            "type": "string"
                                        }
                                    },
                                    "required": [
                                        "person",
                                        "companyName"
                                    ],
                                    "type": "object"
                                },
                                "cce": {
                                    "properties": {
                                        "person": {
                                            "properties": {
                                                "person": {
                                                    "$ref": "#/$defs/person"
                                                },
                                                "constructionid": {
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "person",
                                                "constructionid"
                                            ],
                                            "type": "object"
                                        },
                                        "companyName": {
                                            "type": "string"
                                        }
                                    },
                                    "required": [
                                        "person",
                                        "companyName"
                                    ],
                                    "type": "object"
                                }
                            },
                            "required": [
                                "sce",
                                "cce"
                            ],
                            "type": "object"
                        }
                            """;
        JSONAssert.assertEquals(expectedResponseString, response, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testPersonAddtoListToolInputSchema() throws NoSuchMethodException, SecurityException {
        ToolMetadata toolMetadata = findTool("addPersonToList");
        String response = registry.getToolInputSchema(toolMetadata).toString();
        String expectedResponseString = """
                                                {
                                                "$defs": {
                                                    "address": {
                                                        "properties": {
                                                            "number": {
                                                                "type": "integer"
                                                            },
                                                            "street": {
                                                                "description": "A street object to represent complex streets",
                                                                "properties": {
                                                                    "streetName": {
                                                                        "type": "string"
                                                                    },
                                                                    "roadType": {
                                                                        "type": "string"
                                                                    }
                                                                },
                                                                "required": [
                                                                    "streetName"
                                                                ],
                                                                "type": "object"
                                                            },
                                                            "postcode": {
                                                                "type": "string"
                                                            }
                                                        },
                                                        "required": [
                                                            "number",
                                                            "street",
                                                            "postcode"
                                                        ],
                                                        "type": "object"
                                                    },
                                                    "person": {
                                                        "description": "A person object contains address, company objects",
                                                        "properties": {
                                                            "address": {
                                                                "$ref": "#/$defs/address"
                                                            },
                                                            "company": {
                                                                "properties": {
                                                                    "address": {
                                                                        "$ref": "#/$defs/address"
                                                                    },
                                                                    "name": {
                                                                        "type": "string"
                                                                    },
                                                                    "employees": {
                                                                        "description": "A list of employees (person object)",
                                                                        "items": {
                                                                            "$ref": "#/$defs/person"
                                                                        },
                                                                        "type": "array"
                                                                    },
                                                                    "employeeRegistry": {
                                                                        "properties": {
                                                                            "value": {
                                                                                "$ref": "#/$defs/person"
                                                                            },
                                                                            "key": {
                                                                                "type": "integer"
                                                                            }
                                                                        },
                                                                        "required": [],
                                                                        "type": "object"
                                                                    }
                                                                },
                                                                "required": [
                                                                    "name",
                                                                    "address",
                                                                    "employees",
                                                                    "employeeRegistry"
                                                                ],
                                                                "type": "object"
                                                            },
                                                            "fullname": {
                                                                "type": "string"
                                                            }
                                                        },
                                                        "required": [
                                                            "fullname",
                                                            "address",
                                                            "company"
                                                        ],
                                                        "type": "object"
                                                    }
                                                },
                                                "properties": {
                                                    "employeeList": {
                                                        "description": "List of people",
                                                        "items": {
                                                            "$ref": "#/$defs/person"
                                                        },
                                                        "type": "array"
                                                    },
                                                    "person": {
                                                        "$ref": "#/$defs/person",
                                                        "description": "Person object"
                                                    }
                                                },
                                                "required": [
                                                    "employeeList",
                                                    "person"
                                                ],
                                                "type": "object"
                                            }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testPersonAddtoListToolOutputSchema() throws NoSuchMethodException, SecurityException {
        ToolMetadata toolMetadata = findTool("addPersonToList");
        String response = registry.getToolOuputSchema(toolMetadata).toString();
        String expectedResponseString = """
                            {
                            "$defs": {
                                "address": {
                                    "properties": {
                                        "number": {
                                            "type": "integer"
                                        },
                                        "street": {
                                            "description": "A street object to represent complex streets",
                                            "properties": {
                                                "streetName": {
                                                    "type": "string"
                                                },
                                                "roadType": {
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "streetName"
                                            ],
                                            "type": "object"
                                        },
                                        "postcode": {
                                            "type": "string"
                                        }
                                    },
                                    "required": [
                                        "number",
                                        "street",
                                        "postcode"
                                    ],
                                    "type": "object"
                                },
                                "person": {
                                    "description": "A person object contains address, company objects",
                                    "properties": {
                                        "address": {
                                            "$ref": "#/$defs/address"
                                        },
                                        "company": {
                                            "properties": {
                                                "address": {
                                                    "$ref": "#/$defs/address"
                                                },
                                                "name": {
                                                    "type": "string"
                                                },
                                                "employees": {
                                                    "description": "A list of employees (person object)",
                                                    "items": {
                                                        "$ref": "#/$defs/person"
                                                    },
                                                    "type": "array"
                                                },
                                                "employeeRegistry": {
                                                    "properties": {
                                                        "value": {
                                                            "$ref": "#/$defs/person"
                                                        },
                                                        "key": {
                                                            "type": "integer"
                                                        }
                                                    },
                                                    "required": [],
                                                    "type": "object"
                                                }
                                            },
                                            "required": [
                                                "name",
                                                "address",
                                                "employees",
                                                "employeeRegistry"
                                            ],
                                            "type": "object"
                                        },
                                        "fullname": {
                                            "type": "string"
                                        }
                                    },
                                    "required": [
                                        "fullname",
                                        "address",
                                        "company"
                                    ],
                                    "type": "object"
                                }
                            },
                            "items": {
                                "$ref": "#/$defs/person"
                            },
                            "type": "array"
                        }
                                                    """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    public enum TestEnum {
        VALUE1,
        @JsonbProperty("altValue2")
        VALUE2
    }

    public record EnumHolder(TestEnum testEnum) {}

    @Test
    public void testEnumObject() {
        String schema = registry.getSchema(EnumHolder.class, SchemaDirection.INPUT).toString();

        String expectedSchema = """
                        {
                            "type": "object",
                            "properties": {
                                "testEnum": {
                                    "type": "string",
                                    "enum": [
                                        "VALUE1",
                                        "altValue2"
                                    ]
                                }
                            },
                            "required": ["testEnum"]
                        }""";

        JSONAssert.assertEquals(expectedSchema, schema, true);
    }

    public record EnumMapHolder(Map<TestEnum, String> map) {};

    @Test
    public void testEnumKeyedMap() {
        String schema = registry.getSchema(EnumMapHolder.class, SchemaDirection.INPUT).toString();

        String expectedSchema = """
                        {
                            "type": "object",
                            "properties": {
                                "map": {
                                    "type": "object",
                                    "additionalProperties": {
                                        "type": "string"
                                    },
                                    "propertyNames": {
                                        "type": "string",
                                        "enum": [
                                            "VALUE1",
                                            "altValue2"
                                        ]
                                    }
                                }
                            },
                            "required": ["map"]
                        }""";
        JSONAssert.assertEquals(expectedSchema, schema, true);
    }

    @Test
    public void testOptionalPartialPersonSchema() {
        String response = registry.getSchema(partialPerson.class, SchemaDirection.INPUT).toString();
        String expectedResponseString = """
                            {
                            "$defs": {
                                "address": {
                                    "properties": {
                                        "number": {
                                            "type": "integer"
                                        },
                                        "street": {
                                            "description": "A street object to represent complex streets",
                                            "properties": {
                                                "streetName": {
                                                    "type": "string"
                                                },
                                                "roadType": {
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "streetName"
                                            ],
                                            "type": "object"
                                        },
                                        "postcode": {
                                            "type": "string"
                                        }
                                    },
                                    "required": [
                                        "number",
                                        "street",
                                        "postcode"
                                    ],
                                    "type": "object"
                                },
                                "partialPerson": {
                                    "type": "object",
                                    "properties": {
                                        "name": {"type": "string"},
                                        "address": {"$ref": "#/$defs/address"},
                                        "partialCompany": {
                                            "type": "object",
                                            "properties": {
                                                "name": {"type": "string"},
                                                "address": {"$ref": "#/$defs/address"},
                                                "employees": {
                                                    "type": "array",
                                                    "description": "A list of employees (person object)",
                                                    "items": {"$ref": "#/$defs/partialPerson"}
                                                },
                                                "employeeRegistry": {
                                                    "type": "object",
                                                    "additionalProperties": {"$ref": "#/$defs/partialPerson"}
                                                }
                                            },
                                            "required": []
                                        }
                                    },
                                    "required": [
                                        "name",
                                        "partialCompany"
                                    ]
                                }
                            },
                            "$ref": "#/$defs/partialPerson"
                        }
                            """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    public static class BoxMap<K, V, T> {
        K key;
        V value;
        T type;

        /**
         * @return the key
         */
        public K getKey() {
            return key;
        }

        /**
         * @param key the key to set
         */
        public void setKey(K key) {
            this.key = key;
        }

        /**
         * @return the value
         */
        public V getValue() {
            return value;
        }

        /**
         * @param value the value to set
         */
        public void setValue(V value) {
            this.value = value;
        }

        /**
         * @return the type
         */
        public T getType() {
            return type;
        }

        /**
         * @param type the type to set
         */
        public void setType(T type) {
            this.type = type;
        }

    }

    public static class Container {
        BoxMap<String, String, Integer> bm;

        /**
         * @return the bm
         */
        public BoxMap<String, String, Integer> getBm() {
            return bm;
        }

        /**
         * @param bm the bm to set
         */
        public void setBm(BoxMap<String, String, Integer> bm) {
            this.bm = bm;
        }

    }

    @Test
    public void testConcreteParamterizedGenericClass() {
        String response = registry.getSchema(Container.class, SchemaDirection.INPUT).toString();
        String expectedResponseString = """
                            {
                            "type": "object",
                            "properties": {
                                "bm": {
                                    "type": "object",
                                    "properties": {
                                        "type": {
                                            "type": "integer"
                                        },
                                        "value": {
                                            "type": "string"
                                        },
                                        "key": {
                                            "type": "string"
                                        }
                                    },
                                    "required": [
                                        "type",
                                        "value",
                                        "key"
                                    ]
                                }
                            },
                            "required": [
                                "bm"
                            ]
                        }
                            """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Tool(name = "addGenericToList", title = "adds generic to generic list", description = "adds person to employee list, returns nothing")
    public @Schema(description = "Returns list of person object") <T> List<T> addGenericToList(@ToolArg(name = "generic list",
                                                                                                        description = "List of generics") List<T> list,
                                                                                               @ToolArg(name = "generic", description = "Generic object") T item) {
        list.add(item);
        return list;
        //comment
    }

    @Test
    public void testGenericToolArg() {
        ToolMetadata toolMetadata = findTool("addGenericToList");
        String response = registry.getToolInputSchema(toolMetadata).toString();
        String expectedResponseString = """
                            {
                            "type": "object",
                            "properties": {
                                "generic list": {
                                    "type": "array",
                                    "items": {
                                        "$ref": "#/$defs/T"
                                    },
                                    "description": "List of generics"
                                },
                                "generic": {
                                    "$ref": "#/$defs/T",
                                    "description": "Generic object"
                                }
                            },
                            "required": [
                                "generic list",
                                "generic"
                            ],
                            "$defs": {
                                "T": {
                                    "type": "object"
                                }
                            }
                        }
                            """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Tool(name = "addGenericSingleBoundToList", title = "adds generic to generic list", description = "adds person to employee list, returns nothing")
    public @Schema(description = "Returns list of person object") <T extends Number> List<T> addGenericSingleBoundToList(@ToolArg(name = "generic list",
                                                                                                                                  description = "List of generics") List<T> list,
                                                                                                                         @ToolArg(name = "generic",
                                                                                                                                  description = "Generic object") T item) {
        list.add(item);
        return list;
        //comment
    }

    @Test
    public void testGenericSingleBoundToolArg() {
        ToolMetadata toolMetadata = findTool("addGenericSingleBoundToList");
        String response = registry.getToolInputSchema(toolMetadata).toString();
        String expectedResponseString = """
                            {
                            "type": "object",
                            "properties": {
                                "generic list": {
                                    "type": "array",
                                    "items": {
                                        "$ref": "#/$defs/T"
                                    },
                                    "description": "List of generics"
                                },
                                "generic": {
                                    "$ref": "#/$defs/T",
                                    "description": "Generic object"
                                }
                            },
                            "required": [
                                "generic list",
                                "generic"
                            ],
                            "$defs": {
                                "T": {
                                    "type": "number"
                                }
                            }
                        }
                            """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    public static interface NumberRestrictor {
        public Number getMax();

        public Number getMin();

        public void setMax(Number number);

        public void setMin(Number number);
    }

    @Tool(name = "addGenericMultipleBoundsToList", title = "adds generic to generic list", description = "adds person to employee list, returns nothing")
    public @Schema(description = "Returns list of person object") <T extends Number & NumberRestrictor> List<T> addGenericMultipleBoundsToList(@ToolArg(name = "generic list",
                                                                                                                                                        description = "List of generics") List<T> list,
                                                                                                                                               @ToolArg(name = "generic",
                                                                                                                                                        description = "Generic object") T item) {
        list.add(item);
        return list;
        //comment
    }

    @Test
    public void testGenericMultipleBoundsToolArg() {
        ToolMetadata toolMetadata = findTool("addGenericMultipleBoundsToList");
        String response = registry.getToolInputSchema(toolMetadata).toString();
        String expectedResponseString = """
                            {
                            "type": "object",
                            "properties": {
                                "generic list": {
                                    "type": "array",
                                    "items": {
                                        "$ref": "#/$defs/T"
                                    },
                                    "description": "List of generics"
                                },
                                "generic": {
                                    "$ref": "#/$defs/T",
                                    "description": "Generic object"
                                }
                            },
                            "required": [
                                "generic list",
                                "generic"
                            ],
                            "$defs": {
                                "T": {
                                    "allOf": [
                                        {
                                            "type": "number"
                                        },
                                        {
                                            "type": "object",
                                            "properties": {
                                                "min": {
                                                    "type": "number"
                                                },
                                                "max": {
                                                    "type": "number"
                                                }
                                            },
                                            "required": [
                                                "min",
                                                "max"
                                            ]
                                        }
                                    ]
                                }
                            }
                        }
                            """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Tool(name = "addWildcardToList", title = "adds wildcard to wildcard list", description = "adds person to employee list, returns nothing")
    public @Schema(description = "Returns list of person object") List<?> addWildcardToList(@ToolArg(name = "wildcard list",
                                                                                                     description = "List of wildcard") List<?> list,
                                                                                            @ToolArg(name = "number", description = "number") Number number) {
        return null;
        //comment
    }

    @Test
    public void testWildcardToolArg() {
        ToolMetadata toolMetadata = findTool("addWildcardToList");
        String response = registry.getToolInputSchema(toolMetadata).toString();
        String expectedResponseString = """
                        {
                            "type": "object",
                            "properties": {
                                "number": {
                                    "type": "number",
                                    "description": "number"
                                },
                                "wildcard list": {
                                    "type": "array",
                                    "items": {
                                        "type": "object"
                                    },
                                    "description": "List of wildcard"
                                }
                            },
                            "required": [
                                "number",
                                "wildcard list"
                            ]
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Tool(name = "addWildcardExtendBoundToList", title = "adds wildcard to wildcard list", description = "adds person to employee list, returns nothing")
    public @Schema(description = "Returns list of person object") List<? extends Number> addWildcardExtendBoundToList(@ToolArg(name = "wildcard list",
                                                                                                                               description = "List of wildcard") List<? extends NumberRestrictor> list,
                                                                                                                      @ToolArg(name = "number",
                                                                                                                               description = "number") Number number) {
        return null;
        //comment
    }

    @Test
    public void testGenericExtendBoundToolArg() {
        ToolMetadata toolMetadata = findTool("addWildcardExtendBoundToList");
        String response = registry.getToolInputSchema(toolMetadata).toString();
        String expectedResponseString = """
                                    {
                                        "type": "object",
                                        "properties": {
                                            "number": {
                                                "type": "number",
                                                "description": "number"
                                            },
                                            "wildcard list": {
                                                "type": "array",
                                                "items": {
                                                    "type": "object",
                                                    "properties": {
                                                        "min": {
                                                            "type": "number"
                                                        },
                                                        "max": {
                                                            "type": "number"
                                                        }
                                                    },
                                                    "required": [
                                                        "min",
                                                        "max"
                                                    ]
                                                },
                                                "description": "List of wildcard"
                                            }
                                        },
                                        "required": [
                                            "number",
                                            "wildcard list"
                                        ]
                                    }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Tool(name = "addWildcardSuperBoundsToList", title = "adds wildcard to wildcard list", description = "adds person to employee list, returns nothing")
    public @Schema(description = "Returns list of person object") List<? super Integer> addWildcardSuperBoundsToList(@ToolArg(name = "wildcard list",
                                                                                                                              description = "List of wildcard") List<? super NumberRestrictor> list,
                                                                                                                     @ToolArg(name = "number",
                                                                                                                              description = "number") Number number) {
        return null;
        //comment
    }

    @Test
    public void testWildcardSuperBoundsToolArg() {
        ToolMetadata toolMetadata = findTool("addWildcardSuperBoundsToList");
        String response = registry.getToolInputSchema(toolMetadata).toString();
        String expectedResponseString = """
                                    {
                                        "type": "object",
                                        "properties": {
                                            "number": {
                                                "type": "number",
                                                "description": "number"
                                            },
                                            "wildcard list": {
                                                "type": "array",
                                                "items": {
                                                    "type": "object"
                                                },
                                                "description": "List of wildcard"
                                            }
                                        },
                                        "required": [
                                            "number",
                                            "wildcard list"
                                        ]
                                    }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Tool(name = "addGenericToGenericArray", title = "adds generic to generic Array", description = "adds person to Generic Array, returns nothing")
    public @Schema(description = "Returns list of person object") <T> List<T> addGenericToGenericArray(@ToolArg(name = "generic list 1",
                                                                                                                description = "List of generics 1") T[] list1,
                                                                                                       @ToolArg(name = "generic list 2",
                                                                                                                description = "List of generics 1 ") List<T>[] list2,
                                                                                                       @ToolArg(name = "generic", description = "Generic object") T item) {
        return null;
        //comment
    }

    @Test
    public void testGenericArrayToolArg() {
        ToolMetadata toolMetadata = findTool("addGenericToGenericArray");
        String response = registry.getToolInputSchema(toolMetadata).toString();
        String expectedResponseString = """
                                    {
                                        "type": "object",
                                        "properties": {
                                            "generic list 2": {
                                                "type": "array",
                                                "items": {
                                                    "type": "array",
                                                    "items": {
                                                        "$ref": "#/$defs/T"
                                                    }
                                                },
                                                "description": "List of generics 1 "
                                            },
                                            "generic": {
                                                "$ref": "#/$defs/T",
                                                "description": "Generic object"
                                            },
                                            "generic list 1": {
                                                "type": "array",
                                                "items": {
                                                    "$ref": "#/$defs/T"
                                                },
                                                "description": "List of generics 1"
                                            }
                                        },
                                        "required": [
                                            "generic list 2",
                                            "generic",
                                            "generic list 1"
                                        ],
                                        "$defs": {
                                            "T": {
                                                "type": "object"
                                            }
                                        }
                                    }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

}
