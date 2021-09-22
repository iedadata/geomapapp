package haxby.image.jcodec.codecs.vpx;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import haxby.image.jcodec.common.AudioCodecMeta;
import haxby.image.jcodec.common.Codec;
import haxby.image.jcodec.common.Muxer;
import haxby.image.jcodec.common.MuxerTrack;
import haxby.image.jcodec.common.VideoCodecMeta;
import haxby.image.jcodec.common.io.NIOUtils;
import haxby.image.jcodec.common.io.SeekableByteChannel;
import haxby.image.jcodec.common.model.Packet;
import haxby.image.jcodec.common.model.Size;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class IVFMuxer implements Muxer, MuxerTrack {

    private SeekableByteChannel ch;
    private int nFrames;
    private Size dim;
    private int frameRate;
    private boolean headerWritten;

    public IVFMuxer(SeekableByteChannel ch) throws IOException {
        this.ch = ch;
    }

    @Override
    public void addFrame(Packet pkt) throws IOException {
        if (!headerWritten) {
            frameRate = pkt.getTimescale();
            writeHeader();
            headerWritten = true;
        }

        ByteBuffer fh = ByteBuffer.allocate(12);
        fh.order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer frame = pkt.getData();
        fh.putInt(frame.remaining());
        fh.putLong(nFrames);
        fh.clear();

        ch.write(fh);
        ch.write(frame);

        nFrames++;
    }

    public void close() throws IOException {
        ch.setPosition(24);
        NIOUtils.writeIntLE(ch, nFrames);
    }

    @Override
    public MuxerTrack addVideoTrack(Codec codec, VideoCodecMeta meta) {
        if (dim != null)
            throw new RuntimeException("IVF can not have multiple video tracks.");
        this.dim = meta.getSize();
        return this;
    }

    private void writeHeader() throws IOException {
        ByteBuffer ivf = ByteBuffer.allocate(32);
        ivf.order(ByteOrder.LITTLE_ENDIAN);

        ivf.put((byte) 'D');
        ivf.put((byte) 'K');
        ivf.put((byte) 'I');
        ivf.put((byte) 'F');
        ivf.putShort((short) 0);/* version */
        ivf.putShort((short) 32); /* headersize */
        ivf.putInt(0x30385056); /* headersize */
        ivf.putShort((short) dim.getWidth()); /* width */
        ivf.putShort((short) dim.getHeight()); /* height */
        ivf.putInt(frameRate); /* rate */
        ivf.putInt(1); /* scale */
        ivf.putInt(1); /* length */
        ivf.clear();

        ch.write(ivf);
    }

    @Override
    public MuxerTrack addAudioTrack(Codec codec, AudioCodecMeta meta) {
        throw new RuntimeException("Video-only container");
    }

    @Override
    public void finish() throws IOException {
    }
}