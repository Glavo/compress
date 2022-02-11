package org.apache.commons.compress.compressors.snappy;

import org.apache.commons.compress.compressors.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FramedSnappyCompressor extends BuiltinCompressor {
    public FramedSnappyCompressor() {
        super(CompressorStreamFactory.SNAPPY_FRAMED);
    }

    @Override
    public boolean matches(byte[] signature, int length) {
        return FramedSnappyCompressorInputStream.matches(signature, length);
    }

    @Override
    protected CompressorInputStream internalCreateCompressorInputStream(InputStream in, boolean decompressUntilEOF, int memoryLimitInKb) throws IOException, CompressorException {
        return new FramedSnappyCompressorInputStream(in);
    }

    @Override
    protected CompressorOutputStream internalCreateCompressorOutputStream(OutputStream out) throws IOException, CompressorException {
        return new FramedSnappyCompressorOutputStream(out);
    }
}
