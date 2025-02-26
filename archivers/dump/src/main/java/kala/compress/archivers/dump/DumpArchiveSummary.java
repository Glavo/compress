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
package kala.compress.archivers.dump;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.attribute.FileTime;
import java.util.Objects;

/**
 * This class represents identifying information about a Dump archive volume. It consists the archive's dump time, label, hostname, device name and possibly
 * last mount point plus the volume's volume id and first record number.
 * <p>
 * For the corresponding C structure see the header of {@link DumpArchiveEntry}.
 * </p>
 */
public class DumpArchiveSummary {

    private long dumpTime;
    private long previousDumpTime;
    private int volume;
    private String label;
    private int level;
    private String filesys;
    private String devname;
    private String hostname;
    private int flags;
    private int firstrec;
    private int ntrec;

    DumpArchiveSummary(final byte[] buffer, final Charset encoding) throws IOException {
        dumpTime = 1000L * DumpArchiveUtil.convert32(buffer, 4);
        previousDumpTime = 1000L * DumpArchiveUtil.convert32(buffer, 8);
        volume = DumpArchiveUtil.convert32(buffer, 12);
        label = DumpArchiveUtil.decode(encoding, buffer, 676, DumpArchiveConstants.LBLSIZE).trim();
        level = DumpArchiveUtil.convert32(buffer, 692);
        filesys = DumpArchiveUtil.decode(encoding, buffer, 696, DumpArchiveConstants.NAMELEN).trim();
        devname = DumpArchiveUtil.decode(encoding, buffer, 760, DumpArchiveConstants.NAMELEN).trim();
        hostname = DumpArchiveUtil.decode(encoding, buffer, 824, DumpArchiveConstants.NAMELEN).trim();
        flags = DumpArchiveUtil.convert32(buffer, 888);
        firstrec = DumpArchiveUtil.convert32(buffer, 892);
        ntrec = DumpArchiveUtil.convert32(buffer, 896);

        // extAttributes = DumpArchiveUtil.convert32(buffer, 900);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DumpArchiveSummary other = (DumpArchiveSummary) obj;
        return Objects.equals(devname, other.devname) && dumpTime == other.dumpTime && Objects.equals(hostname, other.hostname);
    }

    /**
     * Gets the device name, e.g., /dev/sda3 or /dev/mapper/vg0-home.
     *
     * @return device name
     */
    public String getDevname() {
        return devname;
    }

    /**
     * Gets the time of this dump.
     *
     * @return the time of this dump.
     * @since 1.27.1-0
     */
    public FileTime getDumpTime() {
        return FileTime.fromMillis(dumpTime);
    }

    /**
     * Gets the last mountpoint, e.g., /home.
     *
     * @return last mountpoint
     */
    public String getFilesystem() {
        return filesys;
    }

    /**
     * Gets the inode of the first record on this volume.
     *
     * @return inode of the first record on this volume.
     */
    public int getFirstRecord() {
        return firstrec;
    }

    /**
     * Gets the miscellaneous flags. See below.
     *
     * @return flags
     */
    public int getFlags() {
        return flags;
    }

    /**
     * Gets the hostname of the system where the dump was performed.
     *
     * @return hostname the host name
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Gets dump label. This may be autogenerated, or it may be specified by the user.
     *
     * @return dump label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Gets the level of this dump. This is a number between 0 and 9, inclusive, and a level 0 dump is a complete dump of the partition. For any other dump 'n'
     * this dump contains all files that have changed since the last dump at this level or lower. This is used to support different levels of incremental
     * backups.
     *
     * @return dump level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Gets the number of records per tape block. This is typically between 10 and 32.
     *
     * @return the number of records per tape block
     */
    public int getNTRec() {
        return ntrec;
    }

    /**
     * Gets the time of the previous dump at this level higher.
     *
     * @return dump time may be null
     * @since 1.27.1-0
     */
    public FileTime getPreviousDumpTime() {
        return FileTime.fromMillis(previousDumpTime);
    }

    /**
     * Gets volume (tape) number.
     *
     * @return volume (tape) number.
     */
    public int getVolume() {
        return volume;
    }

    @Override
    public int hashCode() {
        return Objects.hash(devname, dumpTime, hostname);
    }

    /**
     * Is this volume compressed? N.B., individual blocks may or may not be compressed. The first block is never compressed.
     *
     * @return true if volume is compressed
     */
    public boolean isCompressed() {
        return (flags & 0x0080) == 0x0080;
    }

    /**
     * Does this volume contain extended attributes.
     *
     * @return true if volume contains extended attributes.
     */
    public boolean isExtendedAttributes() {
        return (flags & 0x8000) == 0x8000;
    }

    /**
     * Does this volume only contain metadata?
     *
     * @return true if volume only contains meta-data
     */
    public boolean isMetaDataOnly() {
        return (flags & 0x0100) == 0x0100;
    }

    /**
     * Is this the new header format? (We do not currently support the old format.)
     *
     * @return true if using new header format
     */
    public boolean isNewHeader() {
        return (flags & 0x0001) == 0x0001;
    }

    /**
     * Is this the new inode format? (We do not currently support the old format.)
     *
     * @return true if using new inode format
     */
    public boolean isNewInode() {
        return (flags & 0x0002) == 0x0002;
    }

    /**
     * Sets the device name.
     *
     * @param devname the device name
     */
    public void setDevname(final String devname) {
        this.devname = devname;
    }

    /**
     * Sets dump time.
     *
     * @param dumpTime the dump time
     * @since 1.27.1-0
     */
    public void setDumpTime(final FileTime dumpTime) {
        this.dumpTime = dumpTime.toMillis();
    }

    /**
     * Sets the last mountpoint.
     *
     * @param fileSystem the last mountpoint
     */
    public void setFilesystem(final String fileSystem) {
        this.filesys = fileSystem;
    }

    /**
     * Sets the inode of the first record.
     *
     * @param firstrec the first record
     */
    public void setFirstRecord(final int firstrec) {
        this.firstrec = firstrec;
    }

    /**
     * Sets the miscellaneous flags.
     *
     * @param flags flags
     */
    public void setFlags(final int flags) {
        this.flags = flags;
    }

    /**
     * Sets the hostname.
     *
     * @param hostname the host name
     */
    public void setHostname(final String hostname) {
        this.hostname = hostname;
    }

    /**
     * Sets dump label.
     *
     * @param label the label
     */
    public void setLabel(final String label) {
        this.label = label;
    }

    /**
     * Sets level.
     *
     * @param level the level
     */
    public void setLevel(final int level) {
        this.level = level;
    }

    /**
     * Sets the number of records per tape block.
     *
     * @param ntrec the number of records per tape block
     */
    public void setNTRec(final int ntrec) {
        this.ntrec = ntrec;
    }

    /**
     * Sets previous dump time.
     *
     * @param previousDumpTime the previous dump dat
     * @since 1.27.1-0
     */
    public void setPreviousDumpTime(final FileTime previousDumpTime) {
        this.previousDumpTime = previousDumpTime.toMillis();
    }

    /**
     * Sets volume (tape) number.
     *
     * @param volume the volume number
     */
    public void setVolume(final int volume) {
        this.volume = volume;
    }
}
