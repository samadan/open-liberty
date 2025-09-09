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
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import io.openliberty.mcp.annotations.Schema;
import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.internal.ToolMetadata;
import io.openliberty.mcp.internal.schemas.SchemaDirection;
import io.openliberty.mcp.internal.schemas.SchemaRegistryTwo;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;

/**
 *
 */
public class SchemaTestTwo {
    static SchemaRegistryTwo registry;

    public static record person(@JsonbProperty("fullname") String name, address address, company company) {};

    public static record address(int number, street street, String postcode, @JsonbTransient String directions) {};

    @Schema("{\"properties\": {  \"streetName\": { \"type\": \"string\" }, \"roadType\": { \"type\": \"string\" } }, \"required\": [ \"streetName\" ], \"type\": \"object\"}")
    public static record street(String streetname, String roadtype) {}

    public static record company(String name, address address, List<person> employees,
                                 @Schema(value = "{\"properties\": {\"key\":{ \"type\": \"integer\" }, \"value\":{ \"$ref\": \"#/$defs/company/employess/items\" }},\"required\": [ ], \"type\": \"object\"}") Map<String, person> employeeRegistry) {};

    public static class SoftwareCompanyEntry {
        SoftwareCompanyEntry.person person;
        String companyName;

        public SoftwareCompanyEntry(SoftwareCompanyEntry.person person, String companyName) {}

        public static record person(SchemaTestTwo.person person, int softwareid) {}
    };

    public static class ConstructionCompanyEntry {
        ConstructionCompanyEntry.person person;
        String companyName;

        public ConstructionCompanyEntry(ConstructionCompanyEntry.person person, String companyName) {}

        public static record person(SchemaTestTwo.person person, String constructionid) {}
    };

    public static class PortfolioEntry {
        SoftwareCompanyEntry sce;
        ConstructionCompanyEntry cce;

        public PortfolioEntry(SoftwareCompanyEntry sce, ConstructionCompanyEntry cce) {}

    };

    @Tool(name = "checkPerson", title = "checks if person is in employee list", description = "Returns boolean")
    public boolean checkPerson(@ToolArg(name = "person", description = "Person object") person person, @ToolArg(name = "company", description = "Company object") company company) {
        return true;
    }

    @Tool(name = "addPersonToList", title = "adds person to employee list", description = "adds person to employee list, returns nothing")
    public void addPersonToList(@ToolArg(name = "employeeList", description = "List of people") List<person> employeeList,
                                @ToolArg(name = "person", description = "Person object") person person) {
        //comment
    }

    @BeforeClass
    public static void setup() {
        registry = new SchemaRegistryTwo();
        SchemaRegistryTwo.set(registry);
    }

    @Test
    public void testPersonSchema() {
        String response = SchemaRegistryTwo.getSchema(person.class, SchemaDirection.INPUT);
        System.out.println(response);
        String expectedResponseString = """
                        {
                          "$defs": {
                            "address": {
                              "properties": {
                                "number": {
                                  "type": "integer"
                                },
                                "street": {
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
                                      "items": {
                                        "$ref": "#/$defs/person"
                                      },
                                      "type": "array"
                                    },
                                    "employeeRegistry": {
                                      "properties": {
                                        "value": {
                                          "$ref": "#/$defs/company/employess/items"
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

        String toolInputSchema = SchemaRegistryTwo.getToolInputSchema(toolMetadata);
        String expectedSchema = """
                        {
                        "type" : "object",
                        "properties" : {
                            "id" : {
                                "type": "number",
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

        String toolInputSchema = SchemaRegistryTwo.getToolOuputSchema(toolMetadata);
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
        List<ToolMetadata> tools = Arrays.stream(SchemaTestTwo.class.getDeclaredMethods())
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
        String toolInputSchema = SchemaRegistryTwo.getToolInputSchema(toolMetadata);

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
        String toolInputSchema = SchemaRegistryTwo.getToolOuputSchema(toolMetadata);

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

//    @Test
//    public void testPersonCheckToolSchema() throws NoSuchMethodException, SecurityException {
//        String response = SchemaRegistry.generateSchema(this.getClass().getDeclaredMethod("checkPerson", person.class, company.class));
//        String expectedResponseString = """
//                        {
//                          "$defs": {
//                            "address": {
//                              "properties": {
//                                "number": {
//                                  "type": "integer"
//                                },
//                                "street": {
//                                  "properties": {
//                                    "streetName": {
//                                      "type": "string"
//                                    },
//                                    "roadType": {
//                                      "type": "string"
//                                    }
//                                  },
//                                  "required": [
//                                    "streetName"
//                                  ],
//                                  "type": "object"
//                                },
//                                "postcode": {
//                                  "type": "string"
//                                }
//                              },
//                              "required": [
//                                "number",
//                                "street",
//                                "postcode"
//                              ],
//                              "type": "object"
//                            },
//                            "person": {
//                              "description": "Person object",
//                              "properties": {
//                                "address": {
//                                  "$ref": "#/$defs/address"
//                                },
//                                "company": {
//                                  "$ref": "#/$defs/company"
//                                },
//                                "fullname": {
//                                  "type": "string"
//                                }
//                              },
//                              "required": [
//                                "fullname",
//                                "address",
//                                "company"
//                              ],
//                              "type": "object"
//                            },
//                            "company": {
//                              "description": "Company object",
//                              "properties": {
//                                "address": {
//                                  "$ref": "#/$defs/address"
//                                },
//                                "name": {
//                                  "type": "string"
//                                },
//                                "employees": {
//                                  "items": {
//                                    "$ref": "#/$defs/person"
//                                  },
//                                  "type": "array"
//                                },
//                                "employeeRegistry": {
//                                  "properties": {
//                                    "value": {
//                                      "$ref": "#/$defs/company/employess/items"
//                                    },
//                                    "key": {
//                                      "type": "integer"
//                                    }
//                                  },
//                                  "required": [],
//                                  "type": "object"
//                                }
//                              },
//                              "required": [
//                                "name",
//                                "address",
//                                "employees",
//                                "employeeRegistry"
//                              ],
//                              "type": "object"
//                            }
//                          },
//                          "description": "Returns boolean",
//                          "properties": {
//                            "person": {
//                              "$ref": "#/$defs/person"
//                            },
//                            "company": {
//                              "$ref": "#/$defs/company"
//                            }
//                          },
//                          "required": [
//                            "person",
//                            "company"
//                          ],
//                          "type": "object"
//                        }
//                                                                """;
//        JSONAssert.assertEquals(expectedResponseString, response, true);
//    }
//
//    @Test
//    public void testPersonCheckToolPersonParamSchema() throws NoSuchMethodException, SecurityException {
//        String response = SchemaRegistry.generateSchema(this.getClass().getDeclaredMethod("checkPerson", person.class, company.class).getParameters()[0]);
//        String expectedResponseString = """
//                        {
//                          "$defs": {
//                            "address": {
//                              "properties": {
//                                "number": {
//                                  "type": "integer"
//                                },
//                                "street": {
//                                  "properties": {
//                                    "streetName": {
//                                      "type": "string"
//                                    },
//                                    "roadType": {
//                                      "type": "string"
//                                    }
//                                  },
//                                  "required": [
//                                    "streetName"
//                                  ],
//                                  "type": "object"
//                                },
//                                "postcode": {
//                                  "type": "string"
//                                }
//                              },
//                              "required": [
//                                "number",
//                                "street",
//                                "postcode"
//                              ],
//                              "type": "object"
//                            },
//                            "person": {
//                              "description": "Person object",
//                              "properties": {
//                                "address": {
//                                  "$ref": "#/$defs/address"
//                                },
//                                "company": {
//                                  "properties": {
//                                    "address": {
//                                      "$ref": "#/$defs/address"
//                                    },
//                                    "name": {
//                                      "type": "string"
//                                    },
//                                    "employees": {
//                                      "items": {
//                                        "$ref": "#/$defs/person"
//                                      },
//                                      "type": "array"
//                                    },
//                                    "employeeRegistry": {
//                                      "properties": {
//                                        "value": {
//                                          "$ref": "#/$defs/company/employess/items"
//                                        },
//                                        "key": {
//                                          "type": "integer"
//                                        }
//                                      },
//                                      "required": [],
//                                      "type": "object"
//                                    }
//                                  },
//                                  "required": [
//                                    "name",
//                                    "address",
//                                    "employees",
//                                    "employeeRegistry"
//                                  ],
//                                  "type": "object"
//                                },
//                                "fullname": {
//                                  "type": "string"
//                                }
//                              },
//                              "required": [
//                                "fullname",
//                                "address",
//                                "company"
//                              ],
//                              "type": "object"
//                            }
//                          },
//                          "$ref": "#/$defs/person"
//                        }
//                                                                """;
//        JSONAssert.assertEquals(expectedResponseString, response, true);
//    }
//
//    @Test
//    public void testAddressSchema() {
//        String response = SchemaRegistry.generateSchema(address.class);
//        String expectedResponseString = """
//                        {
//                          "properties": {
//                            "number": {
//                              "type": "integer"
//                            },
//                            "street": {
//                              "properties": {
//                                "streetName": {
//                                  "type": "string"
//                                },
//                                "roadType": {
//                                  "type": "string"
//                                }
//                              },
//                              "required": [
//                                "streetName"
//                              ],
//                              "type": "object"
//                            },
//                            "postcode": {
//                              "type": "string"
//                            }
//                          },
//                          "required": [
//                            "number",
//                            "street",
//                            "postcode"
//                          ],
//                          "type": "object"
//                        }
//                        """;
//        JSONAssert.assertEquals(expectedResponseString, response, true);
//    }
//
//    @Test
//    public void testStreetSchema() {
//        String response = SchemaRegistry.generateSchema(street.class);
//        String expectedResponseString = """
//                        {
//                          "properties": {
//                            "streetName": {
//                              "type": "string"
//                            },
//                            "roadType": {
//                              "type": "string"
//                            }
//                          },
//                          "required": [
//                            "streetName"
//                          ],
//                          "type": "object"
//                        }
//                        """;
//        JSONAssert.assertEquals(expectedResponseString, response, true);
//    }
//
//    @Test
//    public void testCompanySchema() {
//        String response = SchemaRegistry.generateSchema(company.class);
//        String expectedResponseString = """
//                        {
//                          "$defs": {
//                            "address": {
//                              "properties": {
//                                "number": {
//                                  "type": "integer"
//                                },
//                                "street": {
//                                  "properties": {
//                                    "streetName": {
//                                      "type": "string"
//                                    },
//                                    "roadType": {
//                                      "type": "string"
//                                    }
//                                  },
//                                  "required": [
//                                    "streetName"
//                                  ],
//                                  "type": "object"
//                                },
//                                "postcode": {
//                                  "type": "string"
//                                }
//                              },
//                              "required": [
//                                "number",
//                                "street",
//                                "postcode"
//                              ],
//                              "type": "object"
//                            },
//                            "company": {
//                              "properties": {
//                                "address": {
//                                  "$ref": "#/$defs/address"
//                                },
//                                "name": {
//                                  "type": "string"
//                                },
//                                "employees": {
//                                  "items": {
//                                    "properties": {
//                                      "address": {
//                                        "$ref": "#/$defs/address"
//                                      },
//                                      "company": {
//                                        "$ref": "#/$defs/company"
//                                      },
//                                      "fullname": {
//                                        "type": "string"
//                                      }
//                                    },
//                                    "required": [
//                                      "fullname",
//                                      "address",
//                                      "company"
//                                    ],
//                                    "type": "object"
//                                  },
//                                  "type": "array"
//                                },
//                                "employeeRegistry": {
//                                  "properties": {
//                                    "value": {
//                                      "$ref": "#/$defs/company/employess/items"
//                                    },
//                                    "key": {
//                                      "type": "integer"
//                                    }
//                                  },
//                                  "required": [],
//                                  "type": "object"
//                                }
//                              },
//                              "required": [
//                                "name",
//                                "address",
//                                "employees",
//                                "employeeRegistry"
//                              ],
//                              "type": "object"
//                            }
//                          },
//                          "$ref": "#/$defs/company"
//                        }
//                        """;
//        JSONAssert.assertEquals(expectedResponseString, response, true);
//    }
//
//    @Test
//    public void testPortfolioEntryDuplicateNameSchema() {
//        String response = SchemaRegistry.generateSchema(PortfolioEntry.class);
//        String expectedResponseString = """
//                        {
//                          "$defs": {
//                            "person2": {
//                              "properties": {
//                                "address": {
//                                  "$ref": "#/$defs/address"
//                                },
//                                "company": {
//                                  "properties": {
//                                    "address": {
//                                      "$ref": "#/$defs/address"
//                                    },
//                                    "name": {
//                                      "type": "string"
//                                    },
//                                    "employees": {
//                                      "items": {
//                                        "$ref": "#/$defs/person2"
//                                      },
//                                      "type": "array"
//                                    },
//                                    "employeeRegistry": {
//                                      "properties": {
//                                        "value": {
//                                          "$ref": "#/$defs/company/employess/items"
//                                        },
//                                        "key": {
//                                          "type": "integer"
//                                        }
//                                      },
//                                      "required": [],
//                                      "type": "object"
//                                    }
//                                  },
//                                  "required": [
//                                    "name",
//                                    "address",
//                                    "employees",
//                                    "employeeRegistry"
//                                  ],
//                                  "type": "object"
//                                },
//                                "fullname": {
//                                  "type": "string"
//                                }
//                              },
//                              "required": [
//                                "fullname",
//                                "address",
//                                "company"
//                              ],
//                              "type": "object"
//                            },
//                            "address": {
//                              "properties": {
//                                "number": {
//                                  "type": "integer"
//                                },
//                                "street": {
//                                  "properties": {
//                                    "streetName": {
//                                      "type": "string"
//                                    },
//                                    "roadType": {
//                                      "type": "string"
//                                    }
//                                  },
//                                  "required": [
//                                    "streetName"
//                                  ],
//                                  "type": "object"
//                                },
//                                "postcode": {
//                                  "type": "string"
//                                }
//                              },
//                              "required": [
//                                "number",
//                                "street",
//                                "postcode"
//                              ],
//                              "type": "object"
//                            }
//                          },
//                          "properties": {
//                            "sce": {
//                              "properties": {
//                                "person": {
//                                  "properties": {
//                                    "softwareid": {
//                                      "type": "integer"
//                                    },
//                                    "person": {
//                                      "$ref": "#/$defs/person2"
//                                    }
//                                  },
//                                  "required": [
//                                    "person",
//                                    "softwareid"
//                                  ],
//                                  "type": "object"
//                                },
//                                "companyName": {
//                                  "type": "string"
//                                }
//                              },
//                              "required": [
//                                "person",
//                                "companyName"
//                              ],
//                              "type": "object"
//                            },
//                            "cce": {
//                              "properties": {
//                                "person": {
//                                  "properties": {
//                                    "person": {
//                                      "$ref": "#/$defs/person2"
//                                    },
//                                    "constructionid": {
//                                      "type": "string"
//                                    }
//                                  },
//                                  "required": [
//                                    "person",
//                                    "constructionid"
//                                  ],
//                                  "type": "object"
//                                },
//                                "companyName": {
//                                  "type": "string"
//                                }
//                              },
//                              "required": [
//                                "person",
//                                "companyName"
//                              ],
//                              "type": "object"
//                            }
//                          },
//                          "required": [
//                            "sce",
//                            "cce"
//                          ],
//                          "type": "object"
//                        }
//                        """;
//        JSONAssert.assertEquals(expectedResponseString, response, true);
//    }
//
//    @Test
//    public void testPersonAddtoListToolSchema() throws NoSuchMethodException, SecurityException {
//        String response = SchemaRegistry.generateSchema(this.getClass().getDeclaredMethod("addPersonToList", List.class, person.class));
//        String expectedResponseString = """
//                        {
//                          "$defs": {
//                            "address": {
//                              "properties": {
//                                "number": {
//                                  "type": "integer"
//                                },
//                                "street": {
//                                  "properties": {
//                                    "streetName": {
//                                      "type": "string"
//                                    },
//                                    "roadType": {
//                                      "type": "string"
//                                    }
//                                  },
//                                  "required": [
//                                    "streetName"
//                                  ],
//                                  "type": "object"
//                                },
//                                "postcode": {
//                                  "type": "string"
//                                }
//                              },
//                              "required": [
//                                "number",
//                                "street",
//                                "postcode"
//                              ],
//                              "type": "object"
//                            },
//                            "person": {
//                              "description": "Person object",
//                              "properties": {
//                                "address": {
//                                  "$ref": "#/$defs/address"
//                                },
//                                "company": {
//                                  "properties": {
//                                    "address": {
//                                      "$ref": "#/$defs/address"
//                                    },
//                                    "name": {
//                                      "type": "string"
//                                    },
//                                    "employees": {
//                                      "items": {
//                                        "$ref": "#/$defs/person"
//                                      },
//                                      "type": "array"
//                                    },
//                                    "employeeRegistry": {
//                                      "properties": {
//                                        "value": {
//                                          "$ref": "#/$defs/company/employess/items"
//                                        },
//                                        "key": {
//                                          "type": "integer"
//                                        }
//                                      },
//                                      "required": [],
//                                      "type": "object"
//                                    }
//                                  },
//                                  "required": [
//                                    "name",
//                                    "address",
//                                    "employees",
//                                    "employeeRegistry"
//                                  ],
//                                  "type": "object"
//                                },
//                                "fullname": {
//                                  "type": "string"
//                                }
//                              },
//                              "required": [
//                                "fullname",
//                                "address",
//                                "company"
//                              ],
//                              "type": "object"
//                            }
//                          },
//                          "description": "adds person to employee list, returns nothing",
//                          "properties": {
//                            "employeeList": {
//                              "description": "List of people",
//                              "items": {
//                                "$ref": "#/$defs/person"
//                              },
//                              "type": "array"
//                            },
//                            "person": {
//                              "$ref": "#/$defs/person"
//                            }
//                          },
//                          "required": [
//                            "employeeList",
//                            "person"
//                          ],
//                          "type": "object"
//                        }
//                                                """;
//        JSONAssert.assertEquals(expectedResponseString, response, true);
//    }
//
//    @Test
//    public void testPersonListAddToolListParamSchema() throws NoSuchMethodException, SecurityException {
//        String response = SchemaRegistry.generateSchema(this.getClass().getDeclaredMethod("addPersonToList", List.class, person.class).getParameters()[0]);
//        String expectedResponseString = """
//                        {
//                          "defs": {
//                            "address": {
//                              "properties": {
//                                "number": {
//                                  "type": "integer"
//                                },
//                                "street": {
//                                  "properties": {
//                                    "streetName": {
//                                      "type": "string"
//                                    },
//                                    "roadType": {
//                                      "type": "string"
//                                    }
//                                  },
//                                  "required": [
//                                    "streetName"
//                                  ],
//                                  "type": "object"
//                                },
//                                "postcode": {
//                                  "type": "string"
//                                }
//                              },
//                              "required": [
//                                "number",
//                                "street",
//                                "postcode"
//                              ],
//                              "type": "object"
//                            },
//                            "person": {
//                              "properties": {
//                                "address": {
//                                  "$ref": "#/$defs/address"
//                                },
//                                "company": {
//                                  "properties": {
//                                    "address": {
//                                      "$ref": "#/$defs/address"
//                                    },
//                                    "name": {
//                                      "type": "string"
//                                    },
//                                    "employees": {
//                                      "items": {
//                                        "$ref": "#/$defs/person"
//                                      },
//                                      "type": "array"
//                                    },
//                                    "employeeRegistry": {
//                                      "properties": {
//                                        "value": {
//                                          "$ref": "#/$defs/company/employess/items"
//                                        },
//                                        "key": {
//                                          "type": "integer"
//                                        }
//                                      },
//                                      "required": [],
//                                      "type": "object"
//                                    }
//                                  },
//                                  "required": [
//                                    "name",
//                                    "address",
//                                    "employees",
//                                    "employeeRegistry"
//                                  ],
//                                  "type": "object"
//                                },
//                                "fullname": {
//                                  "type": "string"
//                                }
//                              },
//                              "required": [
//                                "fullname",
//                                "address",
//                                "company"
//                              ],
//                              "type": "object"
//                            }
//                          },
//                          "description": "List of people",
//                          "items": {
//                            "$ref": "#/$defs/person"
//                          },
//                          "type": "array"
//                        }
//                                                """;
//
//        JSONAssert.assertEquals(expectedResponseString, response, true);
//    }

}
