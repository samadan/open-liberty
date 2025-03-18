package com.ibm.ws.netty.compression;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Test;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.ws.bytebuffer.internal.WsByteBufferImpl;
import com.ibm.wsspi.http.channel.compression.DecompressionHandler;

import io.openliberty.http.netty.compression.HttpContentDecompressor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPOutputStream;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;

public class HttpContentCompressionTest {

    private WsByteBuffer bufferUnderTest;

    @After
    public void tearDown() {
        // If any test allocated a real buffer, release and nullify it.
        if (bufferUnderTest != null) {
            bufferUnderTest.release();
            bufferUnderTest = null;
        }
    }

    @Test(expected = DataFormatException.class)
    public void testGzipDecompressionFailsTolerance() throws Exception {
        String payload = new String(new char[1000]).replace("\0", "a");
        byte[] compressed = compressWithGzip(payload.getBytes("UTF-8"));
        ByteBuffer compressedBuffer = ByteBuffer.wrap(compressed);

        WsByteBufferImpl wsBuffer = new WsByteBufferImpl();
        wsBuffer.setByteBuffer(compressedBuffer);
        bufferUnderTest = wsBuffer;

        HttpChannelConfig config = createMockConfig(true, 1, 0);

        HttpContentDecompressor decompressor = new HttpContentDecompressor();
        decompressor.decompress(wsBuffer, config, "gzip");
    }

    @Test
    public void testDecompressionWithinTolerance() throws DataFormatException {
        WsByteBuffer inputBuffer = createMockInputBuffer(100, true, false);
        WsByteBuffer outputBuffer = mock(WsByteBuffer.class);
        when(outputBuffer.remaining()).thenReturn(100);
        when(outputBuffer.getWrappedByteBuffer()).thenReturn(ByteBuffer.allocate(100));
        doNothing().when(outputBuffer).release();

        DecompressionHandler handler = createMockHandler(Arrays.asList(outputBuffer), 100L, 100L);
        HttpChannelConfig config = createMockConfig(true, 100, 3);

        HttpContentDecompressor decompressor = new HttpContentDecompressor();
        WsByteBuffer result = decompressor.decompress(inputBuffer, config, handler);

        assertNotNull(result);
        assertEquals(100, result.remaining());
    }

    @Test
    public void testNoDecompressionChunksProduced() throws DataFormatException {
        WsByteBuffer inputBuffer = createMockInputBuffer(50, true, false);
        DecompressionHandler handler = createMockHandler(Collections.emptyList(), 50L, 0L);
        HttpChannelConfig config = createMockConfig(true, 100, 3);

        HttpContentDecompressor decompressor = new HttpContentDecompressor();
        WsByteBuffer result = decompressor.decompress(inputBuffer, config, handler);

        assertNotNull(result);
        assertEquals(0, result.remaining());
    }

    @Test
    public void testMultiCycleDecompression() throws DataFormatException {
        WsByteBuffer inputBuffer = mock(WsByteBuffer.class);
        when(inputBuffer.remaining()).thenReturn(60, 40);
        when(inputBuffer.hasRemaining()).thenReturn(true, true, false);
        doNothing().when(inputBuffer).release();

        WsByteBuffer outputBuffer1 = mock(WsByteBuffer.class);
        when(outputBuffer1.remaining()).thenReturn(60);
        // Ensure a valid ByteBuffer is returned.
        when(outputBuffer1.getWrappedByteBuffer()).thenReturn(ByteBuffer.allocate(60));
        doNothing().when(outputBuffer1).release();

        WsByteBuffer outputBuffer2 = mock(WsByteBuffer.class);
        when(outputBuffer2.remaining()).thenReturn(40);
        when(outputBuffer2.getWrappedByteBuffer()).thenReturn(ByteBuffer.allocate(40));
        doNothing().when(outputBuffer2).release();

        DecompressionHandler handler = mock(DecompressionHandler.class);
        when(handler.decompress(any(WsByteBuffer.class))).thenReturn(Arrays.asList(outputBuffer1)).thenReturn(Arrays.asList(outputBuffer2));
        // Use long literals.
        when(handler.getBytesRead()).thenReturn(60L, 100L);
        when(handler.getBytesWritten()).thenReturn(60L, 100L);
        when(handler.isEnabled()).thenReturn(true);

        HttpChannelConfig config = createMockConfig(true, 100, 3);
        HttpContentDecompressor decompressor = new HttpContentDecompressor();
        WsByteBuffer result = decompressor.decompress(inputBuffer, config, handler);

        assertNotNull(result);
        assertEquals(100, result.remaining());
    }

    @Test(expected = DataFormatException.class)
    public void testDecompressionExceedingTolerance() throws DataFormatException {
        WsByteBuffer inputBuffer = createMockInputBuffer(100, true, false);
        WsByteBuffer outputBuffer = mock(WsByteBuffer.class);
        when(outputBuffer.remaining()).thenReturn(200);
        when(outputBuffer.getWrappedByteBuffer()).thenReturn(ByteBuffer.allocate(200));
        doNothing().when(outputBuffer).release();

        DecompressionHandler handler = createMockHandler(Arrays.asList(outputBuffer), 100L, 200L);
        HttpChannelConfig config = createMockConfig(true, 1, 0);

        HttpContentDecompressor decompressor = new HttpContentDecompressor();
        decompressor.decompress(inputBuffer, config, handler);
    }

    // @Test
    // public void testAutoDecompressionDisabled() throws DataFormatException, IOException {
    //     String payload = "This is a test payload that should remain compressed.";
    //     byte[] compressed = compressWithGzip(payload.getBytes("UTF-8"));
    //     ByteBuffer compressedBuffer = ByteBuffer.wrap(compressed);

    //     // Use the lightweight stub.
    //     WsByteBufferImpl wsBuffer = new WsByteBufferImpl();
    //     wsBuffer.setByteBuffer(compressedBuffer);
    //     bufferUnderTest = wsBuffer;

    //     HttpChannelConfig config = createMockConfig(false, 100, 3);

    //     HttpContentDecompressor decompressor = new HttpContentDecompressor();
    //     WsByteBuffer result = decompressor.decompress(wsBuffer, config, "gzip");

    //     ByteBuffer original = wsBuffer.getWrappedByteBuffer();
    //     ByteBuffer output = result.getWrappedByteBuffer();

    //     byte[] originalBytes = new byte[original.remaining()];
    //     original.get(originalBytes);
    //     byte[] outputBytes = new byte[output.remaining()];
    //     output.get(outputBytes);

    //     assertArrayEquals("When auto-decompression is disabled, the output should match the input",
    //                       originalBytes, outputBytes);
    // }

    @Test
    public void testEmptyInputBuffer() throws DataFormatException {
        WsByteBufferImpl inputBuffer = new WsByteBufferImpl();
        ByteBuffer emptyBuffer = ByteBuffer.allocate(0);
        inputBuffer.setByteBuffer(emptyBuffer);
        bufferUnderTest = inputBuffer;

        DecompressionHandler handler = createMockHandler(Collections.emptyList(), 0L, 0L);
        HttpChannelConfig config = createMockConfig(true, 100, 3);

        HttpContentDecompressor decompressor = new HttpContentDecompressor();
        WsByteBuffer result = decompressor.decompress(inputBuffer, config, handler);

        assertNotNull(result);
        assertEquals("The output buffer should be empty if the input is empty", 0, result.remaining());
    }

    private HttpChannelConfig createMockConfig(boolean decompressionEnabled, int ratioLimit, int tolerance) {
        HttpChannelConfig config = mock(HttpChannelConfig.class);
        when(config.isAutoDecompressionEnabled()).thenReturn(decompressionEnabled);
        when(config.getDecompressionRatioLimit()).thenReturn(ratioLimit);
        when(config.getDecompressionTolerance()).thenReturn(tolerance);
        return config;
    }

    private WsByteBuffer createMockInputBuffer(int remaining, Boolean... hasRemainingSequence) {
        WsByteBuffer inputBuffer = mock(WsByteBuffer.class);
        when(inputBuffer.remaining()).thenReturn(remaining);
        when(inputBuffer.hasRemaining()).thenReturn(hasRemainingSequence[0], hasRemainingSequence);
        doNothing().when(inputBuffer).release();
        return inputBuffer;
    }

    private DecompressionHandler createMockHandler(List<WsByteBuffer> outputChunks, long bytesRead, long bytesWritten) throws DataFormatException {
        DecompressionHandler handler = mock(DecompressionHandler.class);
        when(handler.decompress(any(WsByteBuffer.class))).thenReturn(outputChunks);
        when(handler.getBytesRead()).thenReturn(bytesRead);
        when(handler.getBytesWritten()).thenReturn(bytesWritten);
        when(handler.isEnabled()).thenReturn(true);
        return handler;
    }

    byte[] compressWithGzip(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
            gos.write(data);
        }
        return baos.toByteArray();
    }

  
}