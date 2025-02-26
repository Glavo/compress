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
package kala.compress.archivers.examples;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import kala.compress.AbstractTest;
import kala.compress.archivers.ArchiveEntry;
import kala.compress.archivers.ArchiveException;
import kala.compress.archivers.ArchiveOutputStream;
import kala.compress.archivers.ArchiveStreamFactory;
import kala.compress.archivers.StreamingNotSupportedException;
import kala.compress.archivers.sevenz.SevenZArchiveReader;
import kala.compress.archivers.sevenz.SevenZArchiveWriter;
import kala.compress.archivers.tar.TarArchiveReader;
import kala.compress.archivers.zip.ZipArchiveReader;
import org.junit.jupiter.api.Test;

public class ExpanderTest extends AbstractTest {

    private File archive;

    private void assertHelloWorld(final String fileName, final String suffix) throws IOException {
        assertTrue(new File(tempResultDir, fileName).isFile(), fileName + " does not exist");
        final byte[] expected = ("Hello, world " + suffix).getBytes(UTF_8);
        final byte[] actual = Files.readAllBytes(tempResultDir.toPath().resolve(fileName));
        assertArrayEquals(expected, actual);
    }

    private void setup7z() throws IOException {
        archive = newTempFile("test.7z");
        final File dummy = newTempFile("x");
        try (OutputStream o = Files.newOutputStream(dummy.toPath())) {
            o.write(new byte[14]);
        }
        try (SevenZArchiveWriter aos = new SevenZArchiveWriter(archive.toPath())) {
            final File inputFile2 = getTempDirFile();
            aos.putArchiveEntry(aos.createArchiveEntry(inputFile2.toPath(), "a"));
            aos.closeArchiveEntry();
            final File inputFile1 = getTempDirFile();
            aos.putArchiveEntry(aos.createArchiveEntry(inputFile1.toPath(), "a/b"));
            aos.closeArchiveEntry();
            final File inputFile = getTempDirFile();
            aos.putArchiveEntry(aos.createArchiveEntry(inputFile.toPath(), "a/b/c"));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(dummy.toPath(), "a/b/d.txt"));
            aos.write("Hello, world 1".getBytes(UTF_8));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(dummy.toPath(), "a/b/c/e.txt"));
            aos.write("Hello, world 2".getBytes(UTF_8));
            aos.closeArchiveEntry();
            aos.finish();
        }
    }

    private void setupTar() throws IOException, ArchiveException {
        archive = newTempFile("test.tar");
        final File dummy = newTempFile("x");
        try (OutputStream o = Files.newOutputStream(dummy.toPath())) {
            o.write(new byte[14]);
        }
        try (@SuppressWarnings("resource") // Files.newOutputStream result closed by ArchiveOutputStream
        ArchiveOutputStream<ArchiveEntry> aos = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("tar", Files.newOutputStream(archive.toPath()))) {
            File inputFile2 = getTempDirFile();
            aos.putArchiveEntry(aos.createArchiveEntry(inputFile2.toPath(), "a"));
            aos.closeArchiveEntry();
            File inputFile1 = getTempDirFile();
            aos.putArchiveEntry(aos.createArchiveEntry(inputFile1.toPath(), "a/b"));
            aos.closeArchiveEntry();
            File inputFile = getTempDirFile();
            aos.putArchiveEntry(aos.createArchiveEntry(inputFile.toPath(), "a/b/c"));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(dummy.toPath(), "a/b/d.txt"));
            aos.write("Hello, world 1".getBytes(UTF_8));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(dummy.toPath(), "a/b/c/e.txt"));
            aos.write("Hello, world 2".getBytes(UTF_8));
            aos.closeArchiveEntry();
            aos.finish();
        }
    }

    private void setupTarForCompress603() throws IOException, ArchiveException {
        archive = newTempFile("test.tar");
        final File dummy = newTempFile("x");
        try (OutputStream o = Files.newOutputStream(dummy.toPath())) {
            o.write(new byte[14]);
        }
        try (@SuppressWarnings("resource") // Files.newOutputStream result closed by ArchiveOutputStream
        ArchiveOutputStream<ArchiveEntry> aos = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("tar", Files.newOutputStream(archive.toPath()))) {
            File inputFile3 = getTempDirFile();
            aos.putArchiveEntry(aos.createArchiveEntry(inputFile3.toPath(), "./"));
            aos.closeArchiveEntry();
            File inputFile2 = getTempDirFile();
            aos.putArchiveEntry(aos.createArchiveEntry(inputFile2.toPath(), "./a"));
            aos.closeArchiveEntry();
            File inputFile1 = getTempDirFile();
            aos.putArchiveEntry(aos.createArchiveEntry(inputFile1.toPath(), "./a/b"));
            aos.closeArchiveEntry();
            File inputFile = getTempDirFile();
            aos.putArchiveEntry(aos.createArchiveEntry(inputFile.toPath(), "./a/b/c"));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(dummy.toPath(), "./a/b/d.txt"));
            aos.write("Hello, world 1".getBytes(UTF_8));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(dummy.toPath(), "./a/b/c/e.txt"));
            aos.write("Hello, world 2".getBytes(UTF_8));
            aos.closeArchiveEntry();
            aos.finish();
        }
    }

    private void setupZip() throws IOException, ArchiveException {
        archive = newTempFile("test.zip");
        final File dummy = newTempFile("x");
        try (OutputStream o = Files.newOutputStream(dummy.toPath())) {
            o.write(new byte[14]);
        }
        try (@SuppressWarnings("resource") // // Files.newOutputStream result closed by ArchiveOutputStream
        ArchiveOutputStream<ArchiveEntry> aos = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("zip", Files.newOutputStream(archive.toPath()))) {
            File inputFile2 = getTempDirFile();
            aos.putArchiveEntry(aos.createArchiveEntry(inputFile2.toPath(), "a"));
            aos.closeArchiveEntry();
            File inputFile1 = getTempDirFile();
            aos.putArchiveEntry(aos.createArchiveEntry(inputFile1.toPath(), "a/b"));
            aos.closeArchiveEntry();
            File inputFile = getTempDirFile();
            aos.putArchiveEntry(aos.createArchiveEntry(inputFile.toPath(), "a/b/c"));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(dummy.toPath(), "a/b/d.txt"));
            aos.write("Hello, world 1".getBytes(UTF_8));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(dummy.toPath(), "a/b/c/e.txt"));
            aos.write("Hello, world 2".getBytes(UTF_8));
            aos.closeArchiveEntry();
            aos.finish();
        }
    }

    private void setupZip(final String entry) throws IOException, ArchiveException {
        archive = newTempFile("test.zip");
        final File dummy = newTempFile("x");
        try (OutputStream o = Files.newOutputStream(dummy.toPath())) {
            o.write(new byte[14]);
        }
        try (@SuppressWarnings("resource") // Files.newOutputStream result closed by ArchiveOutputStream
        ArchiveOutputStream<ArchiveEntry> aos = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("zip", Files.newOutputStream(archive.toPath()))) {
            aos.putArchiveEntry(aos.createArchiveEntry(dummy.toPath(), entry));
            aos.write("Hello, world 1".getBytes(UTF_8));
            aos.closeArchiveEntry();
            aos.finish();
        }
    }

    @Test
    public void testCompress603Tar() throws IOException, ArchiveException {
        setupTarForCompress603();
        try (TarArchiveReader f = new TarArchiveReader(archive.toPath())) {
            new Expander().expand(f, tempResultDir);
        }
        verifyTargetDir();
    }

    @Test
    public void testFileCantEscapeDoubleDotPath() throws IOException, ArchiveException {
        setupZip("../foo");
        try (ZipArchiveReader f = ZipArchiveReader.builder().setFile(archive).get()) {
            assertThrows(IOException.class, () -> new Expander().expand(f, tempResultDir));
        }
    }

    @Test
    public void testFileCantEscapeDoubleDotPathWithSimilarSibling() throws IOException, ArchiveException {
        final String sibling = tempResultDir.getName() + "x";
        final File s = new File(tempResultDir.getParentFile(), sibling);
        assumeFalse(s.exists());
        s.mkdirs();
        assumeTrue(s.exists());
        setupZip("../" + sibling + "/a");
        try (ZipArchiveReader f = ZipArchiveReader.builder().setFile(archive).get()) {
            assertThrows(IOException.class, () -> new Expander().expand(f, tempResultDir));
        }
    }

    @Test
    public void testFileCantEscapeViaAbsolutePath() throws IOException, ArchiveException {
        setupZip("/tmp/foo");
        try (ZipArchiveReader f = ZipArchiveReader.builder().setFile(archive).get()) {
            assertThrows(IOException.class, () -> new Expander().expand(f, tempResultDir));
        }
        assertFalse(new File(tempResultDir, "tmp/foo").isFile());
    }

    @Test
    public void testSevenZChannelVersion() throws IOException, ArchiveException {
        setup7z();
        try (SeekableByteChannel c = FileChannel.open(archive.toPath(), StandardOpenOption.READ)) {
            new Expander().expand("7z", c, tempResultDir);
        }
        verifyTargetDir();
    }

    @Test
    public void testSevenZFileVersion() throws IOException {
        setup7z();
        try (SevenZArchiveReader file = SevenZArchiveReader.builder().setFile(archive).get()) {
            new Expander().expand(file, tempResultDir);
        }
        verifyTargetDir();
    }

    @Test
    public void testSevenZInputStreamVersion() throws IOException {
        setup7z();
        try (InputStream i = new BufferedInputStream(Files.newInputStream(archive.toPath()))) {
            assertThrows(StreamingNotSupportedException.class, () -> new Expander().expand("7z", i, tempResultDir));
        }
    }

    @Test
    public void testSevenZInputStreamVersionWithAutoDetection() throws IOException {
        setup7z();
        try (InputStream i = new BufferedInputStream(Files.newInputStream(archive.toPath()))) {
            assertThrows(StreamingNotSupportedException.class, () -> new Expander().expand(i, tempResultDir));
        }
    }

    @Test
    public void testSevenZTwoFileVersion() throws IOException, ArchiveException {
        setup7z();
        new Expander().expand("7z", archive, tempResultDir);
        verifyTargetDir();
    }

    @Test
    public void testSevenZTwoFileVersionWithAutoDetection() throws IOException, ArchiveException {
        setup7z();
        new Expander().expand(archive, tempResultDir);
        verifyTargetDir();
    }

    @Test
    public void testTarFileVersion() throws IOException, ArchiveException {
        setupTar();
        try (TarArchiveReader f = new TarArchiveReader(archive.toPath())) {
            new Expander().expand(f, tempResultDir);
        }
        verifyTargetDir();
    }

    @Test
    public void testZipFileVersion() throws IOException, ArchiveException {
        setupZip();
        try (ZipArchiveReader f = ZipArchiveReader.builder().setFile(archive).get()) {
            new Expander().expand(f, tempResultDir);
        }
        verifyTargetDir();
    }

    private void verifyTargetDir() throws IOException {
        assertTrue(new File(tempResultDir, "a").isDirectory(), "a has not been created");
        assertTrue(new File(tempResultDir, "a/b").isDirectory(), "a/b has not been created");
        assertTrue(new File(tempResultDir, "a/b/c").isDirectory(), "a/b/c has not been created");
        assertHelloWorld("a/b/d.txt", "1");
        assertHelloWorld("a/b/c/e.txt", "2");
    }

}
