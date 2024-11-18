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
package org.apache.commons.compress.compressors.xz;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.compress.compressors.FileNameUtil;

/**
 * Utility code for the XZ compression format.
 *
 * @ThreadSafe
 * @since 1.4
 */
public class XZUtils {

    private static final FileNameUtil fileNameUtil;

    /**
     * XZ Header Magic Bytes begin a XZ file.
     *
     * <p>
     * This is a copy of {@code org.tukaani.xz.XZ.HEADER_MAGIC} in XZ for Java version 1.5.
     * </p>
     */
    private static final byte[] HEADER_MAGIC = { (byte) 0xFD, '7', 'z', 'X', 'Z', '\0' };

    static {
        final Map<String, String> uncompressSuffix = new HashMap<>();
        uncompressSuffix.put(".txz", ".tar");
        uncompressSuffix.put(".xz", "");
        uncompressSuffix.put("-xz", "");
        fileNameUtil = new FileNameUtil(uncompressSuffix, ".xz");
    }

    /**
     * Maps the given file name to the name that the file should have after compression with xz. Common file types with custom suffixes for compressed versions
     * are automatically detected and correctly mapped. For example the name "package.tar" is mapped to "package.txz". If no custom mapping is applicable, then
     * the default ".xz" suffix is appended to the file name.
     *
     * @param fileName name of a file
     * @return name of the corresponding compressed file
     * @since 1.25.0
     */
    public static String getCompressedFileName(final String fileName) {
        return fileNameUtil.getCompressedFileName(fileName);
    }

    /**
     * Maps the given name of a xz-compressed file to the name that the file should have after uncompression. Commonly used file type specific suffixes like
     * ".txz" are automatically detected and correctly mapped. For example the name "package.txz" is mapped to "package.tar". And any file names with the
     * generic ".xz" suffix (or any other generic xz suffix) is mapped to a name without that suffix. If no xz suffix is detected, then the file name is
     * returned unmapped.
     *
     * @param fileName name of a file
     * @return name of the corresponding uncompressed file
     * @since 1.25.0
     */
    public static String getUncompressedFileName(final String fileName) {
        return fileNameUtil.getUncompressedFileName(fileName);
    }

    /**
     * Detects common xz suffixes in the given file name.
     *
     * @param fileName name of a file
     * @return {@code true} if the file name has a common xz suffix, {@code false} otherwise
     * @since 1.25.0
     */
    public static boolean isCompressedFileName(final String fileName) {
        return fileNameUtil.isCompressedFileName(fileName);
    }

    /**
     * Are the classes required to support XZ compression available?
     *
     * @since 1.5
     * @return true if the classes required to support XZ compression are available
     */
    public static boolean isXZCompressionAvailable() {
        try {
            XZCompressorInputStream.matches(null, 0);
            return true;
        } catch (final NoClassDefFoundError error) { // NOSONAR
            return false;
        }
    }

    /**
     * Checks if the signature matches what is expected for a .xz file.
     *
     * <p>
     * This is more or less a copy of the version found in {@link XZCompressorInputStream} but doesn't depend on the presence of XZ for Java.
     * </p>
     *
     * @param signature the bytes to check
     * @param length    the number of bytes to check
     * @return true if signature matches the .xz magic bytes, false otherwise
     * @since 1.9
     */
    public static boolean matches(final byte[] signature, final int length) {
        if (length < HEADER_MAGIC.length) {
            return false;
        }

        for (int i = 0; i < HEADER_MAGIC.length; ++i) {
            if (signature[i] != HEADER_MAGIC[i]) {
                return false;
            }
        }

        return true;
    }

    /** Private constructor to prevent instantiation of this utility class. */
    private XZUtils() {
    }
}
