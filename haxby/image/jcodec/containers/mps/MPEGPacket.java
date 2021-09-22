package haxby.image.jcodec.containers.mps;

import java.nio.ByteBuffer;

import haxby.image.jcodec.common.model.Packet;
import haxby.image.jcodec.common.model.TapeTimecode;

public class MPEGPacket extends Packet {
    private long offset;
    private ByteBuffer seq;
    private int gop;
    private int timecode;

    public MPEGPacket(ByteBuffer data, long pts, int timescale, long duration, long frameNo, FrameType keyFrame,
            TapeTimecode tapeTimecode) {
        super(data, pts, timescale, duration, frameNo, keyFrame, tapeTimecode, 0);
    }

    public long getOffset() {
        return offset;
    }

    public ByteBuffer getSeq() {
        return seq;
    }

    public int getGOP() {
        return gop;
    }

    public int getTimecode() {
        return timecode;
    }
}