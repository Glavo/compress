/*
 * Copyright 2024 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kala.compress.archivers.jar;

import kala.compress.archivers.ArchiveInputStream;
import kala.compress.archivers.ArchiveOutputStream;
import kala.compress.archivers.ArchiveStreamFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * @author Glavo
 * @since 1.21.0.1
 */
final class JarArchiver extends ArchiveStreamFactory.BuiltinArchiver {
    public JarArchiver() {
        super(ArchiveStreamFactory.JAR);
    }

    @Override
    public boolean matches(byte[] signature, int length) {
        return JarArchiveInputStream.matches(signature, length);
    }

    @Override
    public ArchiveInputStream<?> createArchiveInputStream(InputStream in, Charset charset) {
        return new JarArchiveInputStream(in, charset);
    }

    @Override
    public boolean isOutputAvailable() {
        return true;
    }

    @Override
    public ArchiveOutputStream<?> createArchiveOutputStream(OutputStream out, Charset charset) {
        return new JarArchiveOutputStream(out, charset);
    }
}
