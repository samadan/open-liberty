/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mpRestClient.fat.multipartClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Assert;
import org.junit.Test;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Test for multipart form data with MP Rest Client
 */
public class EntityPartTest {

    /**
     * Tests that a single file is upload. The response is a simple JSON response with the file information.
     *
     * @throws Exception if a test error occurs
     */
    @Test
    public void uploadFile() throws Exception {
        try (FileManagerClient client = createClient()) {
            final byte[] content;
            try (InputStream in = EntityPartTest.class.getResourceAsStream("/multipart/test-file1.txt")) {
                Assert.assertNotNull("Could not find /multipart/test-file1.txt", in);
                content = in.readAllBytes();
            }
            // Send in an InputStream to ensure it works with an InputStream
            final List<EntityPart> files = List.of(EntityPart.withFileName("test-file1.txt")
                    .content(new ByteArrayInputStream(content))
                    .mediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                    .build());
            try (Response response = client.uploadFile(files)) {
                Assert.assertEquals(201, response.getStatus());
                final JsonArray jsonArray = response.readEntity(JsonArray.class);
                Assert.assertNotNull(jsonArray);
                Assert.assertEquals(1, jsonArray.size());
                final JsonObject json = jsonArray.getJsonObject(0);
                Assert.assertEquals("test-file1.txt", json.getString("name"));
                Assert.assertEquals("test-file1.txt", json.getString("fileName"));
                Assert.assertEquals("This is a test file for file 1.", json.getString("content"));
            }
        }
    }

    /**
     * Tests that two files are upload. The response is a simple JSON response with the file information.
     *
     * @throws Exception if a test error occurs
     */
    @Test
    public void uploadMultipleFiles() throws Exception {
        try (FileManagerClient client = createClient()) {
            final Map<String, byte[]> entityPartContent = new LinkedHashMap<>(2);
            try (InputStream in = EntityPartTest.class.getResourceAsStream("/multipart/test-file1.txt")) {
                Assert.assertNotNull("Could not find /multipart/test-file1.txt", in);
                entityPartContent.put("test-file1.txt", in.readAllBytes());
            }
            try (InputStream in = EntityPartTest.class.getResourceAsStream("/multipart/test-file2.txt")) {
                Assert.assertNotNull("Could not find /multipart/test-file2.txt", in);
                entityPartContent.put("test-file2.txt", in.readAllBytes());
            }
            final List<EntityPart> files = entityPartContent.entrySet()
                    .stream()
                    .map((entry) -> {
                        try {
                            return EntityPart.withName(entry.getKey())
                                    .fileName(entry.getKey())
                                    .content(entry.getValue())
                                    .mediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                                    .build();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .collect(Collectors.toList());

            try (Response response = client.uploadFile(files)) {
                Assert.assertEquals(201, response.getStatus());
                final JsonArray jsonArray = response.readEntity(JsonArray.class);
                Assert.assertNotNull(jsonArray);
                Assert.assertEquals(2, jsonArray.size());
                // Don't assume the results are in a specific order
                for (JsonValue value : jsonArray) {
                    final JsonObject json = value.asJsonObject();
                    if (json.getString("name").equals("test-file1.txt")) {
                        Assert.assertEquals("test-file1.txt", json.getString("fileName"));
                        Assert.assertEquals("This is a test file for file 1.", json.getString("content"));
                    } else if (json.getString("name").equals("test-file2.txt")) {
                        Assert.assertEquals("test-file2.txt", json.getString("fileName"));
                        Assert.assertEquals("This is a test file for file 2.", json.getString("content"));
                    } else {
                        Assert.fail(String.format("Unexpected entry %s in JSON response: %n%s", json, jsonArray));
                    }
                }
            }
        }
    }

    private static FileManagerClient createClient() {
        return RestClientBuilder.newBuilder()
                // Fake URI as we use a filter to short-circuit the request
                .baseUri(URI.create("http://localhost:8020"))
                .register(new FileManagerFilter())
                .build(FileManagerClient.class);
    }

    public static class FileManagerFilter implements ClientRequestFilter {

        @Override
        public void filter(final ClientRequestContext requestContext) throws IOException {
            if (requestContext.getMethod().equals("POST")) {
                // Download the file
                @SuppressWarnings("unchecked")
                final List<EntityPart> entityParts = (List<EntityPart>) requestContext.getEntity();
                final JsonArrayBuilder jsonBuilder = Json.createArrayBuilder();
                for (EntityPart part : entityParts) {
                    final JsonObjectBuilder jsonPartBuilder = Json.createObjectBuilder();
                    jsonPartBuilder.add("name", part.getName());
                    if (part.getFileName().isPresent()) {
                        jsonPartBuilder.add("fileName", part.getFileName().get());
                    } else {
                        throw new BadRequestException("No file name for entity part " + part);
                    }
                    jsonPartBuilder.add("content", part.getContent(String.class));
                    jsonBuilder.add(jsonPartBuilder);
                }
                requestContext.abortWith(Response.status(201).entity(jsonBuilder.build()).build());
            } else {
                requestContext
                        .abortWith(Response.status(Response.Status.BAD_REQUEST).entity("Invalid request").build());
            }
        }
    }
}

// Made with Bob
