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
import java.util.List;
import java.util.Map;

/**
 *
 */
public class ToolResponseResult {
    private boolean isError = false;
    private List<Content> content = new ArrayList<>();

    public ToolResponseResult(List<Content> contents, boolean isError) {
        this.isError = isError;
        this.content = contents != null ? contents : new ArrayList<>();
    }

    public static ToolResponseResult fromText(String text, boolean isError) {
        return new ToolResponseResult(List.of(new TextContent(text)), isError);
    }

    public static ToolResponseResult fromImage(String base64Image, String mimeType, boolean isError) {
        return new ToolResponseResult(List.of(new ImageContent(base64Image, mimeType, null, null)), isError);
    }

    public static ToolResponseResult fromAudio(String base64Audio, String mimeType, boolean isError) {
        return new ToolResponseResult(List.of(new AudioContent(base64Audio, mimeType, null, null)), isError);
    }

    public static ToolResponseResult fromMixed(List<Content> contents, boolean isError) {
        return new ToolResponseResult(contents, isError);
    }

    public List<Content> getContent() {
        return content;
    }

    /**
     * @return the isError
     */
    public boolean getIsError() {
        return isError;
    }

    public static abstract class Content {
        private final String type;

        public Content(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public static class TextContent extends Content {

        public Object getText() {
            return text;
        }

        private final Object text;

        public TextContent(Object text) {
            super("text");
            this.text = text;
        }
    }

    public static class ImageContent extends Content {
        private final String data;
        private final String mimeType;
        private final Map<String, Object> _meta;
        private final Object annotations;

        public ImageContent(String data, String mimeType, Map<String, Object> _meta, Object annotations) {
            super("image");
            this.data = data;
            this.mimeType = mimeType;
            this._meta = _meta;
            this.annotations = annotations;
        }

        public String getData() {
            return data;
        }

        public String getMimeType() {
            return mimeType;
        }

        public Map<String, Object> get_meta() {
            return _meta;
        }

        public Object getAnnotations() {
            return annotations;
        }
    }

    public static class AudioContent extends Content {

        private final String data;
        private final String mimeType;
        private final Map<String, Object> _meta;
        private final Object annotations;

        public AudioContent(String data, String mimeType, Map<String, Object> _meta, Object annotations) {
            super("audio");
            this.data = data;
            this.mimeType = mimeType;
            this._meta = _meta;
            this.annotations = annotations;
        }

        public String getData() {
            return data;
        }

        public String getMimeType() {
            return mimeType;
        }

        public Map<String, Object> get_meta() {
            return _meta;
        }

        public Object getAnnotations() {
            return annotations;
        }
    }
}
