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
package kala.compress.archivers.sevenz;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Usage: archive-name [list]
 */
public class CLI {

    private enum Mode {
        LIST("Analysing") {
            private String getContentMethods(final SevenZArchiveEntry entry) {
                final StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (final SevenZMethodConfiguration m : entry.getContentMethods()) {
                    if (!first) {
                        sb.append(", ");
                    }
                    first = false;
                    sb.append(m.getMethod());
                    if (m.getOptions() != null) {
                        sb.append("(").append(m.getOptions()).append(")");
                    }
                }
                return sb.toString();
            }

            @Override
            public void takeAction(final SevenZArchiveReader archive, final SevenZArchiveEntry entry) {
                System.out.print(entry.getName());
                if (entry.isDirectory()) {
                    System.out.print(" dir");
                } else {
                    System.out.print(" " + entry.getCompressedSize() + "/" + entry.getSize());
                }
                if (entry.getHasLastModifiedTime()) {
                    System.out.print(" " + entry.getLastModifiedTime());
                } else {
                    System.out.print(" no last modified time");
                }
                if (!entry.isDirectory()) {
                    System.out.println(" " + getContentMethods(entry));
                } else {
                    System.out.println();
                }
            }
        };

        private final String message;

        Mode(final String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public abstract void takeAction(SevenZArchiveReader archive, SevenZArchiveEntry entry) throws IOException;
    }

    private static Mode grabMode(final String[] args) {
        if (args.length < 2) {
            return Mode.LIST;
        }
        return Enum.valueOf(Mode.class, args[1].toUpperCase(Locale.ROOT));
    }

    public static void main(final String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            return;
        }
        final Mode mode = grabMode(args);
        System.out.println(mode.getMessage() + " " + args[0]);
        final Path file = Paths.get(args[0]);
        if (!Files.isRegularFile(file)) {
            System.err.println(file + " doesn't exist or is a directory");
        }
        try (SevenZArchiveReader archive = SevenZArchiveReader.builder().setPath(file).get()) {
            SevenZArchiveEntry ae;
            while ((ae = archive.getNextEntry()) != null) {
                mode.takeAction(archive, ae);
            }
        }
    }

    private static void usage() {
        System.out.println("Parameters: archive-name [list]");
    }

}
