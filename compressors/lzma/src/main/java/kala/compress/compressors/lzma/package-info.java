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

/**
 * Provides stream classes using the "stand-alone" LZMA algorithm.
 * <p>
 * The classes in this package are wrappers around stream classes provided by the public domain <a href="https://tukaani.org/xz/java.html">XZ for Java</a>
 * library.
 * </p>
 * <p>
 * In general you should prefer the more modern and robust XZ format over stand-alone LZMA compression.
 * </p>
 */
package kala.compress.compressors.lzma;
