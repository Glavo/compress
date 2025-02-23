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
package kala.compress.compressors.lzma;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class LZMAUtilsTest {

    @Test
    public void testGetCompressedFilename() {
        assertEquals(".lzma", LZMAUtils.getCompressedFileName(""));
        assertEquals(".lzma", LZMAUtils.getCompressedFileName(""));
        assertEquals("x.lzma", LZMAUtils.getCompressedFileName("x"));
        assertEquals("x.lzma", LZMAUtils.getCompressedFileName("x"));

        assertEquals("x.wmf .lzma", LZMAUtils.getCompressedFileName("x.wmf "));
        assertEquals("x.wmf .lzma", LZMAUtils.getCompressedFileName("x.wmf "));
        assertEquals("x.wmf\n.lzma", LZMAUtils.getCompressedFileName("x.wmf\n"));
        assertEquals("x.wmf\n.lzma", LZMAUtils.getCompressedFileName("x.wmf\n"));
        assertEquals("x.wmf.y.lzma", LZMAUtils.getCompressedFileName("x.wmf.y"));
        assertEquals("x.wmf.y.lzma", LZMAUtils.getCompressedFileName("x.wmf.y"));
    }

    @Test
    public void testGetUncompressedFilename() {
        assertEquals("", LZMAUtils.getUncompressedFileName(""));
        assertEquals("", LZMAUtils.getUncompressedFileName(""));
        assertEquals(".lzma", LZMAUtils.getUncompressedFileName(".lzma"));
        assertEquals(".lzma", LZMAUtils.getUncompressedFileName(".lzma"));

        assertEquals("x", LZMAUtils.getUncompressedFileName("x.lzma"));
        assertEquals("x", LZMAUtils.getUncompressedFileName("x.lzma"));
        assertEquals("x", LZMAUtils.getUncompressedFileName("x-lzma"));
        assertEquals("x", LZMAUtils.getUncompressedFileName("x-lzma"));

        assertEquals("x.lzma ", LZMAUtils.getUncompressedFileName("x.lzma "));
        assertEquals("x.lzma ", LZMAUtils.getUncompressedFileName("x.lzma "));
        assertEquals("x.lzma\n", LZMAUtils.getUncompressedFileName("x.lzma\n"));
        assertEquals("x.lzma\n", LZMAUtils.getUncompressedFileName("x.lzma\n"));
        assertEquals("x.lzma.y", LZMAUtils.getUncompressedFileName("x.lzma.y"));
        assertEquals("x.lzma.y", LZMAUtils.getUncompressedFileName("x.lzma.y"));
    }

    @Test
    public void testIsCompressedFilename() {
        assertFalse(LZMAUtils.isCompressedFileName(""));
        assertFalse(LZMAUtils.isCompressedFileName(""));
        assertFalse(LZMAUtils.isCompressedFileName(".lzma"));
        assertFalse(LZMAUtils.isCompressedFileName(".lzma"));

        assertTrue(LZMAUtils.isCompressedFileName("x.lzma"));
        assertTrue(LZMAUtils.isCompressedFileName("x.lzma"));
        assertTrue(LZMAUtils.isCompressedFileName("x-lzma"));
        assertTrue(LZMAUtils.isCompressedFileName("x-lzma"));

        assertFalse(LZMAUtils.isCompressedFileName("xxgz"));
        assertFalse(LZMAUtils.isCompressedFileName("xxgz"));
        assertFalse(LZMAUtils.isCompressedFileName("lzmaz"));
        assertFalse(LZMAUtils.isCompressedFileName("lzmaz"));
        assertFalse(LZMAUtils.isCompressedFileName("xaz"));
        assertFalse(LZMAUtils.isCompressedFileName("xaz"));

        assertFalse(LZMAUtils.isCompressedFileName("x.lzma "));
        assertFalse(LZMAUtils.isCompressedFileName("x.lzma "));
        assertFalse(LZMAUtils.isCompressedFileName("x.lzma\n"));
        assertFalse(LZMAUtils.isCompressedFileName("x.lzma\n"));
        assertFalse(LZMAUtils.isCompressedFileName("x.lzma.y"));
        assertFalse(LZMAUtils.isCompressedFileName("x.lzma.y"));
    }

    @Test
    public void testMatches() {
        final byte[] data = { (byte) 0x5D, 0, 0, };
        assertFalse(LZMAUtils.matches(data, 2));
        assertTrue(LZMAUtils.matches(data, 3));
        assertTrue(LZMAUtils.matches(data, 4));
        data[2] = '0';
        assertFalse(LZMAUtils.matches(data, 3));
    }

}
