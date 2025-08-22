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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.mcp.internal.exceptions.jsonrpc.HttpResponseException;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.requests.McpRequest;
import io.openliberty.mcp.internal.responses.McpErrorResponse;
import io.openliberty.mcp.internal.responses.McpResponse;
import io.openliberty.mcp.internal.responses.McpResultResponse;
import jakarta.json.JsonException;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Represents the current request and allows the response to be sent
 */
public class McpTransport {
    private static final TraceComponent tc = Tr.register(McpTransport.class);
    private static final String EXPECTED_PROTOCOL_VERSION = "2025-06-18";
    private static final String MCP_HEADER = "MCP-Protocol-Version";
    private HttpServletRequest req;
    private HttpServletResponse res;
    private Jsonb jsonb;
    private McpRequest mcpRequest;
    private Writer writer;

    public McpTransport(HttpServletRequest req, HttpServletResponse res, Jsonb jsonb) throws IOException {
        this.req = req;
        this.res = res;
        this.jsonb = jsonb;
        writer = res.getWriter();
    }

    /**
     * Initialises McpTransport
     * Checks if the request is valid so we know if further processing can be done
     *
     * @return true if initialisation is successful, false otherwise.
     * @throws IOException if an I/O exception occurs.
     */
    public void init() throws IOException {
        if (!validReqAcceptHeader()) {
            throw new HttpResponseException(HttpServletResponse.SC_NOT_ACCEPTABLE);
        }
        this.mcpRequest = toRequest();
        if (!validProtcolVersionHeader()) {
            throw new HttpResponseException(
                                            HttpServletResponse.SC_BAD_REQUEST,
                                            "Missing or invalid MCP-Protocol-Version header. Expected: " + EXPECTED_PROTOCOL_VERSION);
        }
    }

    /**
     * Converts a HTTP request to an MCP request.
     *
     * @param req The incoming HTTP request.
     * @return An McpRequest object if the conversion is successful.
     * @throws IOException If an I/O error occurs while reading the request.
     * @throws JSONRPCException If the request body is not a valid MCP request.
     */
    @FFDCIgnore(JsonException.class)
    private McpRequest toRequest() throws IOException, JSONRPCException {
        McpRequest mcpRequest = null;
        // TODO: validate headers/contentType etc.
        try {
            BufferedReader re = req.getReader();
            mcpRequest = McpRequest.createValidMCPRequest(re);
        } catch (JsonbException | JsonException e) {
            throw new JSONRPCException(JSONRPCErrorCode.PARSE_ERROR, List.of(e.getMessage()));
        }
        return mcpRequest;
    }

    /**
     * Calculates whether the client accepts MCP's required content types.
     *
     * @throws IOException
     */
    private boolean validReqAcceptHeader() throws IOException {
        try {
            String reqHeaderAcceptedTypes = req.getHeader("Accept");
            if (reqHeaderAcceptedTypes == null)
                return false;
            List<String> requiredMcpMimes = List.of("text/event-stream", "application/json");
            for (String mcpMime : requiredMcpMimes) {
                if (!HeaderValidation.acceptContains(reqHeaderAcceptedTypes, mcpMime))
                    return false;
            }

            return true;
        } catch (Exception e) {
            sendError(e);
            return false;
        }
    }

    private boolean validProtcolVersionHeader() {
        if (!"initialize".equals(mcpRequest.method())) {
            String protocolVersion = req.getHeader(MCP_HEADER);
            if (protocolVersion == null || !protocolVersion.equals(EXPECTED_PROTOCOL_VERSION))
                return false;
        }
        return true;
    }

    public McpRequest getMcpRequest() {
        return this.mcpRequest;
    }

    /**
     * Deserialises the MCP request params value from JSON into an object of the specified type
     *
     * @param <T> the target type to map the JSON into
     * @param type the class we want to deserialise the JSON into
     * @return the MCP request params as an object of the specified type
     */
    public <T> T getParams(Class<T> type) {
        return mcpRequest.getParams(type, jsonb);
    }

    /**
     * Sends the MCP Server's response with the results for the client's request
     *
     * @param result the result field to include in the MCP Server response
     * @throws IOException
     */
    public void sendResponse(Object result) {
        McpResponse mcpResponse = new McpResultResponse(mcpRequest.id(), result);
        jsonb.toJson(mcpResponse, writer);
    }

    /**
     * Sends a 202 response for requests that don't expect anything to be sent back
     *
     * E.g. when a MCP client sends a notification, it'a not expecting anything back in the response
     */
    public void sendEmptyResponse() {
        res.setStatus(HttpServletResponse.SC_ACCEPTED);
    }

    /**
     * sends custom error message response back to client depending on the error
     *
     * @param e the exception that was caught
     * @throws IOException
     */
    public void sendError(Exception e) throws IOException {
        Tr.error(tc, "Unexpected Server Error. Method={0} RequestURI={1} RequestQuery={2}", req.getMethod(), req.getRequestURI(), req.getQueryString());
        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.");
    }

    /**
     * This method is responsible for sending a JSON-RPC error response.
     * It takes a JSONRPCException as an argument and constructs an McpErrorResponse object.
     * This error response is then serialised to JSON and written to the provided writer.
     *
     * @param e The JSONRPCException to be included in the error response.
     */
    public void sendJsonRpcException(JSONRPCException e) {
        McpResponse mcpResponse = new McpErrorResponse(mcpRequest == null ? "" : mcpRequest.id(), e);
        jsonb.toJson(mcpResponse, writer);
    }

    /**
     * Sends a response back to client when an HTTP exception occurs
     *
     * @param e The HttpResponseException to be included in the error response.
     * @throws IOException
     */
    public void sendHttpException(HttpResponseException e) throws IOException {
        res.setStatus(e.getStatusCode());
        if (e.getHeaders() != null)
            e.getHeaders().forEach((key, val) -> res.setHeader(key, val));
        if (e.getMessage() != null) {
            res.setContentType("text/plain");
            writer.write(e.getMessage());
        }
    }
}
