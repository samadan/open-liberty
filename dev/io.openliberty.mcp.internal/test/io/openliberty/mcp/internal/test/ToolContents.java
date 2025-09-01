/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Map;

import org.junit.Test;

import io.openliberty.mcp.content.AudioContent;
import io.openliberty.mcp.content.ImageContent;
import io.openliberty.mcp.content.TextContent;
import io.openliberty.mcp.meta.MetaKey;

public class ToolContents {

    @Test
    public void testTextContentConstructorAndFields() {
        TextContent content = new TextContent("Hello world");

        assertEquals("Hello world", content.text());
        assertNull(content._meta());
        assertEquals(TextContent.Type.TEXT, content.type());
        assertSame(content, content.asText());
    }

    @Test
    public void testTextContentWithMeta() {
        Map<MetaKey, Object> meta = Map.of();
        TextContent content = new TextContent("Text with meta", meta);

        assertEquals("Text with meta", content.text());
        assertEquals(meta, content._meta());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTextContentNullTextThrowsException() {
        new TextContent(null);
    }

    @Test
    public void testImageContentFields() {
        String base64Image = "base64-image-data";
        ImageContent image = new ImageContent(base64Image, "image/png", null);

        assertEquals("image/png", image.mimeType());
        assertEquals(base64Image, image.data());
        assertNull(image._meta());
        assertEquals(ImageContent.Type.IMAGE, image.type());
        assertSame(image, image.asImage());
    }

    @Test
    public void testAudioContentFields() {
        String base64Audio = "base64-audio-data";
        AudioContent audio = new AudioContent(base64Audio, "audio/mpeg", null);

        assertEquals("audio/mpeg", audio.mimeType());
        assertEquals(base64Audio, audio.data());
        assertNull(audio._meta());
        assertEquals(AudioContent.Type.AUDIO, audio.type());
        assertSame(audio, audio.asAudio());
    }
}
