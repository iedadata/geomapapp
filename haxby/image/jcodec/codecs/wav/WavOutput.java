package haxby.image.jcodec.codecs.wav;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import haxby.image.jcodec.audio.AudioSink;
import haxby.image.jcodec.common.AudioFormat;
import haxby.image.jcodec.common.AudioUtil;
import haxby.image.jcodec.common.io.NIOUtils;
import haxby.image.jcodec.common.io.SeekableByteChannel;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Outputs integer samples into wav file
 * 
 * @author The JCodec project
 */
public class WavOutput implements Closeable {

    protected SeekableByteChannel out;
    protected WavHeader header;
    protected int written;
    protected AudioFormat format;

    public WavOutput(SeekableByteChannel out, AudioFormat format) throws IOException {
        this.out = out;
        this.format = format;
        header = WavHeader.createWavHeader(format, 0);
        header.write(out);
    }

    public void write(ByteBuffer samples) throws IOException {
        written += out.write(samples);
    }

    public void close() throws IOException {
        out.setPosition(0);
        WavHeader.createWavHeader(format, format.bytesToFrames(written)).write(out);
        NIOUtils.closeQuietly(out);
    }

    /**
     * Manages the file resource on top of WavOutput
     */
    public static class WavOutFile extends WavOutput {

        public WavOutFile(File f, AudioFormat format) throws IOException {
            super(NIOUtils.writableChannel(f), format);
        }

        @Override
        public void close() throws IOException {
            super.close();
            NIOUtils.closeQuietly(out);
        }
    }

    /**
     * Supports more high-level float and int array output on top of WavOutput
     */
    public static class Sink implements AudioSink, Closeable {
        private WavOutput out;

        public Sink(WavOutput out) {
            this.out = out;
        }

        public void writeFloat(FloatBuffer data) throws IOException {
            ByteBuffer buf = ByteBuffer.allocate(out.format.samplesToBytes(data.remaining()));
            AudioUtil.fromFloat(data, out.format, buf);
            buf.flip();
            out.write(buf);
        }

        public void write(int[] data, int len) throws IOException {
            // Safety net
            len = Math.min(data.length, len);

            ByteBuffer buf = ByteBuffer.allocate(out.format.samplesToBytes(len));
            AudioUtil.fromInt(data, len, out.format, buf);
            buf.flip();
            out.write(buf);
        }

        public void close() throws IOException {
            out.close();
        }
    }
}