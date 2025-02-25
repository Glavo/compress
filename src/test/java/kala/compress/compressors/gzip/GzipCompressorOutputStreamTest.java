/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package kala.compress.compressors.gzip;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import kala.compress.compressors.gzip.ExtraField.SubField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests {@link GzipCompressorOutputStream}.
 */
public class GzipCompressorOutputStreamTest {

    private static final String EXPECTED_BASE_NAME = "\u6D4B\u8BD5\u4E2D\u6587\u540D\u79F0";
    private static final String EXPECTED_FILE_NAME = EXPECTED_BASE_NAME + ".xml";

    private void testChineseFileName(final String expected, final String sourceFile, final Charset fileNameCharset) throws IOException {
        final Path tempSourceFile = Files.createTempFile(sourceFile, sourceFile);
        Files.write(tempSourceFile, "<text>Hello World!</text>".getBytes(StandardCharsets.ISO_8859_1));
        final Path targetFile = Files.createTempFile(EXPECTED_BASE_NAME, ".gz");
        final GzipParameters parameters = new GzipParameters();
        // If your system is Windows with Chinese, and your file name is Chinese, you need set the fileNameCharset to GBK
        // otherwise your file name is different with use GzipCompressorOutputStream without set GzipParameters
        // and the same situation in Linux, need set the fileNameCharset to UTF-8
        parameters.setFileNameCharset(fileNameCharset);
        assertEquals(fileNameCharset, parameters.getFileNameCharset());
        parameters.setFileName(EXPECTED_FILE_NAME);
        try (OutputStream fos = Files.newOutputStream(targetFile);
                GzipCompressorOutputStream gos = new GzipCompressorOutputStream(fos, parameters)) {
            Files.copy(tempSourceFile, gos);
        }
        try (GzipCompressorInputStream gis = new GzipCompressorInputStream(Files.newInputStream(targetFile))) {
            final byte[] fileNameBytes = gis.getMetaData().getFileName().getBytes(StandardCharsets.ISO_8859_1);
            final String unicodeFileName = new String(fileNameBytes, fileNameCharset);
            assertEquals(expected, unicodeFileName);
        }
    }

    /**
     * Tests Chinese file name for Windows behavior.
     *
     * @throws IOException When the test fails.
     */
    @Test
    public void testChineseFileNameGBK() throws IOException {
        assumeTrue(Charset.isSupported("GBK"));
        testChineseFileName(EXPECTED_FILE_NAME, EXPECTED_FILE_NAME, Charset.forName("GBK"));
    }

    /**
     * Tests Chinese file name for Windows behavior.
     *
     * @throws IOException When the test fails.
     */
    @Test
    public void testChineseFileNameUTF8() throws IOException {
        testChineseFileName(EXPECTED_FILE_NAME, EXPECTED_FILE_NAME, StandardCharsets.UTF_8);
    }

    /**
     * Tests the gzip extra header containing subfields.
     *
     * @throws IOException When the test fails.
     */
    @ParameterizedTest
    // @formatter:off
    @CsvSource({
        "1,      , true",
        "1,     0, false",
        "1, 65531, false",
        "1, 65532, true",
        "2,     0, false",
        "2, 32764, true",
        "2, 32763, false"
    })
    // @formatter:on
    public void testExtraSubfields(final int subFieldCount, final Integer payloadSize, final boolean shouldFail) throws IOException {
        final Path tempSourceFile = Files.createTempFile("test_gzip_extra_", ".txt");
        final Path targetFile = Files.createTempFile("test_gzip_extra_", ".txt.gz");
        Files.write(tempSourceFile, "Hello World!".getBytes(StandardCharsets.ISO_8859_1));
        final GzipParameters parameters = new GzipParameters();
        final ExtraField extra = new ExtraField();
        boolean failed = false;
        final byte[][] payloads = new byte[subFieldCount][];
        for (int i = 0; i < subFieldCount; i++) {
            if (payloadSize != null) {
                payloads[i] = new byte[payloadSize];
                Arrays.fill(payloads[i], (byte) ('a' + i));
            }
            try {
                extra.addSubField("z" + i, payloads[i]);
            } catch (final NullPointerException | IOException e) {
                failed = true;
                break;
            }
        }
        assertEquals(shouldFail, failed, "appending subfield " + (shouldFail ? "succes" : "failure") + " was not expected.");
        if (shouldFail) {
            return;
        }
        parameters.setExtraField(extra);
        try (OutputStream fos = Files.newOutputStream(targetFile);
                GzipCompressorOutputStream gos = new GzipCompressorOutputStream(fos, parameters)) {
            Files.copy(tempSourceFile, gos);
        }
        try (GzipCompressorInputStream gis = new GzipCompressorInputStream(Files.newInputStream(targetFile))) {
            final ExtraField extra2 = gis.getMetaData().getExtraField();
            for (int i = 0; i < subFieldCount; i++) {
                final SubField sf = extra2.getSubFieldAt(i);
                assertEquals("z" + i, sf.getId()); // id was saved/loaded correctly
                assertArrayEquals(payloads[i], sf.getPayload(), "field " + i + " has wrong payload");
            }
            final AtomicInteger i = new AtomicInteger();
            gis.getMetaData().getExtraField().forEach(sf -> {
                assertEquals("z" + i, sf.getId()); // id was saved/loaded correctly
                assertArrayEquals(payloads[i.intValue()], sf.getPayload(), "field " + i + " has wrong payload");
                i.incrementAndGet();
            });
        }
    }

    @Test
    public void testExtraSubfieldsEmpty() {
        final ExtraField extra = new ExtraField();
        assertEquals(0, extra.toByteArray().length);
        assertFalse(extra.iterator().hasNext());
        extra.forEach(e -> {
            fail("Not emprt");
        });
        assertThrows(IndexOutOfBoundsException.class, () -> extra.getSubFieldAt(0));
    }

    private void testFileName(final String expected, final String sourceFile) throws IOException {
        final Path tempSourceFile = Files.createTempFile(sourceFile, sourceFile);
        Files.write(tempSourceFile, "<text>Hello World!</text>".getBytes(StandardCharsets.ISO_8859_1));
        final Path targetFile = Files.createTempFile("test", ".gz");
        final GzipParameters parameters = new GzipParameters();
        parameters.setFileName(sourceFile);
        assertEquals(parameters.getFileName(), parameters.getFileName());
        parameters.setFileName(sourceFile);
        assertEquals(parameters.getFileName(), parameters.getFileName());
        try (OutputStream fos = Files.newOutputStream(targetFile);
                GzipCompressorOutputStream gos = new GzipCompressorOutputStream(fos, parameters)) {
            Files.copy(tempSourceFile, gos);
        }
        try (GzipCompressorInputStream gis = new GzipCompressorInputStream(Files.newInputStream(targetFile))) {
            assertEquals(expected, gis.getMetaData().getFileName());
            GzipParameters gzipParameters = gis.getMetaData();
            assertEquals(expected, gzipParameters.getFileName());
        }
    }

    @Test
    public void testFileNameAscii() throws IOException {
        testFileName("ASCII.xml", "ASCII.xml");
    }

    /**
     * Tests COMPRESS-638. Use {@link GzipParameters#setFileNameCharset(Charset)} if you want non-ISO-8859-1 characters.
     *
     * GZip RFC requires ISO 8859-1 (LATIN-1).
     *
     * @throws IOException When the test fails.
     */
    @Test
    public void testFileNameChinesePercentEncoded() throws IOException {
        // "Test Chinese name"
        testFileName("??????.xml", EXPECTED_FILE_NAME);
    }

}
