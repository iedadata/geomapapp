package haxby.image.jcodec.containers.mp4;
import java.nio.ByteBuffer;

import haxby.image.jcodec.common.model.Packet;
import haxby.image.jcodec.common.model.TapeTimecode;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
public class MP4Packet extends Packet {
    public static MP4Packet createMP4PacketWithTimecode(MP4Packet other, TapeTimecode timecode) {
        return createMP4Packet(other.data, other.pts, other.timescale, other.duration, other.frameNo, other.frameType,
                timecode, other.displayOrder, other.mediaPts, other.entryNo);
    }

    public static MP4Packet createMP4PacketWithData(MP4Packet other, ByteBuffer frm) {
        return createMP4Packet(frm, other.pts, other.timescale, other.duration, other.frameNo, other.frameType,
                other.tapeTimecode, other.displayOrder, other.mediaPts, other.entryNo);
    }

    public static MP4Packet createMP4Packet(ByteBuffer data, long pts, int timescale, long duration, long frameNo,
            FrameType iframe, TapeTimecode tapeTimecode, int displayOrder, long mediaPts, int entryNo) {
        return new MP4Packet(data, pts, timescale, duration, frameNo, iframe, tapeTimecode, displayOrder, mediaPts,
                entryNo, 0, 0, false);
    }

    private long mediaPts;
    private int entryNo;
    private long fileOff;
    private int size;
    private boolean psync;

    public MP4Packet(ByteBuffer data, long pts, int timescale, long duration, long frameNo, FrameType iframe,
            TapeTimecode tapeTimecode, int displayOrder, long mediaPts, int entryNo, long fileOff, int size,
            boolean psync) {
        super(data, pts, timescale, duration, frameNo, iframe, tapeTimecode, displayOrder);
        this.mediaPts = mediaPts;
        this.entryNo = entryNo;
        this.fileOff = fileOff;
        this.size = size;
        this.psync = psync;
    }

    /**
     * Zero-offset sample entry index
     * 
     * @return
     */
    public int getEntryNo() {
        return entryNo;
    }

    public long getMediaPts() {
        return mediaPts;
    }

    public long getFileOff() {
        return fileOff;
    }

    public int getSize() {
        return size;
    }

    public boolean isPsync() {
        return psync;
    }

    public void setMediaPts(long arg) {
        this.mediaPts = arg;
    }

    public void setFrameNo(int arg) {
        this.frameNo = arg;
    }
}