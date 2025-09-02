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

import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import io.openliberty.mcp.annotations.Schema;
import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
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

    @Schema("{ \"$anchor\": \"street\", \"properties\": {  \"streetName\": { \"type\": \"string\" }, \"roadType\": { \"type\": \"string\" } }, \"required\": [ \"streetName\" ], \"type\": \"object\" }")
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
