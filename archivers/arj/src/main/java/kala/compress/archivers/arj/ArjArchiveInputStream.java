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
package kala.compress.archivers.arj;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.zip.CRC32;

import kala.compress.archivers.ArchiveEntry;
import kala.compress.archivers.ArchiveException;
import kala.compress.archivers.ArchiveInputStream;
import kala.compress.utils.BoundedInputStream;
import kala.compress.utils.CRC32VerifyingInputStream;
import kala.compress.utils.IOUtils;

/**
 * Implements the "arj" archive format as an InputStream.
 * <ul>
 * <li><a href="https://github.com/FarGroup/FarManager/blob/master/plugins/multiarc/arc.doc/arj.txt">Reference 1</a></li>
 * <li><a href="http://www.fileformat.info/format/arj/corion.htm">Reference 2</a></li>
 * </ul>
 *
 * @NotThreadSafe
 * @since 1.6
 */
public class ArjArchiveInputStream extends ArchiveInputStream<ArjArchiveEntry> {

    private static final int ARJ_MAGIC_1 = 0x60;
    private static final int ARJ_MAGIC_2 = 0xEA;

    /**
     * Checks if the signature matches what is expected for an arj file.
     *
     * @param signature the bytes to check
     * @param length    the number of bytes to check
     * @return true, if this stream is an arj archive stream, false otherwise
     */
    public static boolean matches(final byte[] signature, final int length) {
        return length >= 2 && (0xff & signature[0]) == ARJ_MAGIC_1 && (0xff & signature[1]) == ARJ_MAGIC_2;
    }

    private final DataInputStream dis;
    private final MainHeader mainHeader;
    private LocalFileHeader currentLocalFileHeader;
    private InputStream currentInputStream;

    /**
     * Constructs the ArjInputStream, taking ownership of the inputStream that is passed in, and using the CP437 character encoding.
     *
     * @param inputStream the underlying stream, whose ownership is taken
     * @throws ArchiveException if an exception occurs while reading
     * @throws java.nio.charset.UnsupportedCharsetException if the current operating environment does not support CP437
     */
    public ArjArchiveInputStream(final InputStream inputStream) throws ArchiveException {
        this(inputStream, null);
    }

    /**
     * Constructs the ArjInputStream, taking ownership of the inputStream that is passed in.
     *
     * @param inputStream the underlying stream, whose ownership is taken
     * @param charset the charset used for file names and comments in the archive. May be {@code null} to use the CP437.
     * @throws ArchiveException if an exception occurs while reading
     * @since 1.27.1-0
     */
    public ArjArchiveInputStream(final InputStream inputStream, final Charset charset) throws ArchiveException {
        super(inputStream, charset != null ? charset : Charset.forName("CP437"));
        in = dis = new DataInputStream(inputStream);
        try {
            mainHeader = readMainHeader();
            if ((mainHeader.arjFlags & MainHeader.Flags.GARBLED) != 0) {
                throw new ArchiveException("Encrypted ARJ files are unsupported");
            }
            if ((mainHeader.arjFlags & MainHeader.Flags.VOLUME) != 0) {
                throw new ArchiveException("Multi-volume ARJ files are unsupported");
            }
        } catch (final IOException ioException) {
            throw new ArchiveException(ioException.getMessage(), ioException);
        }
    }

    @Override
    public boolean canReadEntryData(final ArchiveEntry ae) {
        return ae instanceof ArjArchiveEntry && ((ArjArchiveEntry) ae).getMethod() == LocalFileHeader.Methods.STORED;
    }

    @Override
    public void close() throws IOException {
        dis.close();
    }

    /**
     * Gets the archive's comment.
     *
     * @return the archive's comment
     */
    public String getArchiveComment() {
        return mainHeader.comment;
    }

    /**
     * Gets the archive's recorded name.
     *
     * @return the archive's name
     */
    public String getArchiveName() {
        return mainHeader.name;
    }

    @Override
    public ArjArchiveEntry getNextEntry() throws IOException {
        if (currentInputStream != null) {
            // return value ignored as IOUtils.skip ensures the stream is drained completely
            final InputStream input = currentInputStream;
            IOUtils.skip(input, Long.MAX_VALUE);
            currentInputStream.close();
            currentLocalFileHeader = null;
            currentInputStream = null;
        }

        currentLocalFileHeader = readLocalFileHeader();
        if (currentLocalFileHeader != null) {
            currentInputStream = new BoundedInputStream(dis, currentLocalFileHeader.compressedSize);
            if (currentLocalFileHeader.method == LocalFileHeader.Methods.STORED) {
                currentInputStream = new CRC32VerifyingInputStream(currentInputStream, currentLocalFileHeader.originalSize,
                        currentLocalFileHeader.originalCrc32);
            }
            return new ArjArchiveEntry(currentLocalFileHeader);
        }
        currentInputStream = null;
        return null;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        if (currentLocalFileHeader == null) {
            throw new IllegalStateException("No current arj entry");
        }
        if (currentLocalFileHeader.method != LocalFileHeader.Methods.STORED) {
            throw new IOException("Unsupported compression method " + currentLocalFileHeader.method);
        }
        return currentInputStream.read(b, off, len);
    }

    private int read16(final DataInputStream dataIn) throws IOException {
        final int value = dataIn.readUnsignedShort();
        count(2);
        return Integer.reverseBytes(value) >>> 16;
    }

    private int read32(final DataInputStream dataIn) throws IOException {
        final int value = dataIn.readInt();
        count(4);
        return Integer.reverseBytes(value);
    }

    private int read8(final DataInputStream dataIn) throws IOException {
        final int value = dataIn.readUnsignedByte();
        count(1);
        return value;
    }

    private void readExtraData(final int firstHeaderSize, final DataInputStream firstHeader, final LocalFileHeader localFileHeader) throws IOException {
        if (firstHeaderSize >= 33) {
            localFileHeader.extendedFilePosition = read32(firstHeader);
            if (firstHeaderSize >= 45) {
                localFileHeader.dateTimeAccessed = read32(firstHeader);
                localFileHeader.dateTimeCreated = read32(firstHeader);
                localFileHeader.originalSizeEvenForVolumes = read32(firstHeader);
                pushedBackBytes(12);
            }
            pushedBackBytes(4);
        }
    }

    private byte[] readHeader() throws IOException {
        boolean found = false;
        byte[] basicHeaderBytes = null;
        do {
            int first;
            int second = read8(dis);
            do {
                first = second;
                second = read8(dis);
            } while (first != ARJ_MAGIC_1 && second != ARJ_MAGIC_2);
            final int basicHeaderSize = read16(dis);
            if (basicHeaderSize == 0) {
                // end of archive
                return null;
            }
            if (basicHeaderSize <= 2600) {
                basicHeaderBytes = readRange(dis, basicHeaderSize);
                final long basicHeaderCrc32 = read32(dis) & 0xFFFFFFFFL;
                final CRC32 crc32 = new CRC32();
                crc32.update(basicHeaderBytes);
                if (basicHeaderCrc32 == crc32.getValue()) {
                    found = true;
                }
            }
        } while (!found);
        return basicHeaderBytes;
    }

    private LocalFileHeader readLocalFileHeader() throws IOException {
        final byte[] basicHeaderBytes = readHeader();
        if (basicHeaderBytes == null) {
            return null;
        }
        try (DataInputStream basicHeader = new DataInputStream(new ByteArrayInputStream(basicHeaderBytes))) {

            final int firstHeaderSize = basicHeader.readUnsignedByte();
            final byte[] firstHeaderBytes = readRange(basicHeader, firstHeaderSize - 1);
            pushedBackBytes(firstHeaderBytes.length);
            try (DataInputStream firstHeader = new DataInputStream(new ByteArrayInputStream(firstHeaderBytes))) {

                final LocalFileHeader localFileHeader = new LocalFileHeader();
                localFileHeader.archiverVersionNumber = firstHeader.readUnsignedByte();
                localFileHeader.minVersionToExtract = firstHeader.readUnsignedByte();
                localFileHeader.hostOS = firstHeader.readUnsignedByte();
                localFileHeader.arjFlags = firstHeader.readUnsignedByte();
                localFileHeader.method = firstHeader.readUnsignedByte();
                localFileHeader.fileType = firstHeader.readUnsignedByte();
                localFileHeader.reserved = firstHeader.readUnsignedByte();
                localFileHeader.dateTimeModified = read32(firstHeader);
                localFileHeader.compressedSize = 0xffffFFFFL & read32(firstHeader);
                localFileHeader.originalSize = 0xffffFFFFL & read32(firstHeader);
                localFileHeader.originalCrc32 = 0xffffFFFFL & read32(firstHeader);
                localFileHeader.fileSpecPosition = read16(firstHeader);
                localFileHeader.fileAccessMode = read16(firstHeader);
                pushedBackBytes(20);
                localFileHeader.firstChapter = firstHeader.readUnsignedByte();
                localFileHeader.lastChapter = firstHeader.readUnsignedByte();

                readExtraData(firstHeaderSize, firstHeader, localFileHeader);

                localFileHeader.name = readString(basicHeader);
                localFileHeader.comment = readString(basicHeader);

                final ArrayList<byte[]> extendedHeaders = new ArrayList<>();
                int extendedHeaderSize;
                while ((extendedHeaderSize = read16(dis)) > 0) {
                    final byte[] extendedHeaderBytes = readRange(dis, extendedHeaderSize);
                    final long extendedHeaderCrc32 = 0xffffFFFFL & read32(dis);
                    final CRC32 crc32 = new CRC32();
                    crc32.update(extendedHeaderBytes);
                    if (extendedHeaderCrc32 != crc32.getValue()) {
                        throw new IOException("Extended header CRC32 verification failure");
                    }
                    extendedHeaders.add(extendedHeaderBytes);
                }
                localFileHeader.extendedHeaders = extendedHeaders.toArray(new byte[0][]);

                return localFileHeader;
            }
        }
    }

    private MainHeader readMainHeader() throws IOException {
        final byte[] basicHeaderBytes = readHeader();
        if (basicHeaderBytes == null) {
            throw new IOException("Archive ends without any headers");
        }
        final DataInputStream basicHeader = new DataInputStream(new ByteArrayInputStream(basicHeaderBytes));

        final int firstHeaderSize = basicHeader.readUnsignedByte();
        final byte[] firstHeaderBytes = readRange(basicHeader, firstHeaderSize - 1);
        pushedBackBytes(firstHeaderBytes.length);

        final DataInputStream firstHeader = new DataInputStream(new ByteArrayInputStream(firstHeaderBytes));

        final MainHeader hdr = new MainHeader();
        hdr.archiverVersionNumber = firstHeader.readUnsignedByte();
        hdr.minVersionToExtract = firstHeader.readUnsignedByte();
        hdr.hostOS = firstHeader.readUnsignedByte();
        hdr.arjFlags = firstHeader.readUnsignedByte();
        hdr.securityVersion = firstHeader.readUnsignedByte();
        hdr.fileType = firstHeader.readUnsignedByte();
        hdr.reserved = firstHeader.readUnsignedByte();
        hdr.dateTimeCreated = read32(firstHeader);
        hdr.dateTimeModified = read32(firstHeader);
        hdr.archiveSize = 0xffffFFFFL & read32(firstHeader);
        hdr.securityEnvelopeFilePosition = read32(firstHeader);
        hdr.fileSpecPosition = read16(firstHeader);
        hdr.securityEnvelopeLength = read16(firstHeader);
        pushedBackBytes(20); // count has already counted them via readRange
        hdr.encryptionVersion = firstHeader.readUnsignedByte();
        hdr.lastChapter = firstHeader.readUnsignedByte();

        if (firstHeaderSize >= 33) {
            hdr.arjProtectionFactor = firstHeader.readUnsignedByte();
            hdr.arjFlags2 = firstHeader.readUnsignedByte();
            firstHeader.readUnsignedByte();
            firstHeader.readUnsignedByte();
        }

        hdr.name = readString(basicHeader);
        hdr.comment = readString(basicHeader);

        final int extendedHeaderSize = read16(dis);
        if (extendedHeaderSize > 0) {
            hdr.extendedHeaderBytes = readRange(dis, extendedHeaderSize);
            final long extendedHeaderCrc32 = 0xffffFFFFL & read32(dis);
            final CRC32 crc32 = new CRC32();
            crc32.update(hdr.extendedHeaderBytes);
            if (extendedHeaderCrc32 != crc32.getValue()) {
                throw new IOException("Extended header CRC32 verification failure");
            }
        }

        return hdr;
    }

    private byte[] readRange(final InputStream in, final int len) throws IOException {
        final byte[] b = IOUtils.readRange(in, len);
        count(b.length);
        if (b.length < len) {
            throw new EOFException();
        }
        return b;
    }

    private String readString(final DataInputStream dataIn) throws IOException {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            int nextByte;
            while ((nextByte = dataIn.readUnsignedByte()) != 0) {
                buffer.write(nextByte);
            }
            return buffer.toString(getEncoding().name());
        }
    }
}
