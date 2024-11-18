/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.compress.archivers.zip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.compress.AbstractTempDirTest;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ZipArchiveOutputStream}.
 */
public class ZipArchiveOutputStreamTest extends AbstractTempDirTest {

    @Test
    public void testFileBasics() throws IOException {
        try (ZipArchiveOutputStream stream = new ZipArchiveOutputStream(createTempFile())) {
            assertTrue(stream.isSeekable());
        }
    }

    @Test
    public void testOutputStreamBasics() throws IOException {
        try (ZipArchiveOutputStream stream = new ZipArchiveOutputStream(new ByteArrayOutputStream())) {
            assertFalse(stream.isSeekable());
        }
    }

    @Test
    public void testSetEncoding() throws IOException {
        try (ZipArchiveOutputStream stream = new ZipArchiveOutputStream(createTempFile())) {
            stream.setEncoding(StandardCharsets.UTF_8.name());
            assertEquals(StandardCharsets.UTF_8.name(), stream.getEncoding());
            stream.setEncoding(null);
            assertEquals(Charset.defaultCharset().name(), stream.getEncoding());
        }
    }
}
