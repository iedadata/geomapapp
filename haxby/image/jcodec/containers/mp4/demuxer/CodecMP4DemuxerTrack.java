package haxby.image.jcodec.containers.mp4.demuxer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import haxby.image.jcodec.codecs.aac.AACUtils;
import haxby.image.jcodec.codecs.aac.ADTSParser;
import haxby.image.jcodec.codecs.aac.ADTSParser.Header;
import haxby.image.jcodec.codecs.h264.H264Utils;
import haxby.image.jcodec.codecs.h264.mp4.AvcCBox;
import haxby.image.jcodec.common.Codec;
import haxby.image.jcodec.common.DemuxerTrackMeta;
import haxby.image.jcodec.common.SeekableDemuxerTrack;
import haxby.image.jcodec.common.io.NIOUtils;
import haxby.image.jcodec.common.model.Packet;
import haxby.image.jcodec.containers.mp4.MP4Packet;
import haxby.image.jcodec.containers.mp4.boxes.VideoSampleEntry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Regular MP4 track containing frames
 * 
 * @author The JCodec project
 * 
 */
public class CodecMP4DemuxerTrack implements SeekableDemuxerTrack {

    private ByteBuffer codecPrivate;
    private AvcCBox avcC;
    private SeekableDemuxerTrack other;
    private Codec codec;

    public CodecMP4DemuxerTrack(SeekableDemuxerTrack other) {
        this.other = other;
        DemuxerTrackMeta meta = other.getMeta();
        codec = meta.getCodec();

        if (codec == Codec.H264) {
            avcC = H264Utils.parseAVCC((VideoSampleEntry) ((MP4DemuxerTrackMeta) meta).getSampleEntries()[0]);
        }
        codecPrivate = meta.getCodecPrivate();
    }

    @Override
    public Packet nextFrame() throws IOException {
        Packet nextFrame = other.nextFrame();
        if (nextFrame == null)
            return null;
        ByteBuffer newData = convertPacket(nextFrame.getData());
        return MP4Packet.createMP4PacketWithData((MP4Packet) nextFrame, newData);
    }

    public ByteBuffer convertPacket(ByteBuffer data) {
        if (codecPrivate != null) {
            if (codec == Codec.H264) {
                ByteBuffer annexbCoded = H264Utils.decodeMOVPacket(data, avcC);
                if (H264Utils.isByteBufferIDRSlice(annexbCoded)) {
                    ByteBuffer newData = NIOUtils.combineBuffers(Arrays.asList(codecPrivate, annexbCoded));
                    return newData;
                } else {
                    return annexbCoded;
                }
            } else if (codec == Codec.AAC) {
                Header adts = AACUtils.streamInfoToADTS(codecPrivate, true, 1, data.remaining());
                ByteBuffer adtsRaw = ByteBuffer.allocate(7);
                ADTSParser.write(adts, adtsRaw);
                ByteBuffer newData = NIOUtils.combineBuffers(Arrays.asList(adtsRaw, data));
                return newData;
            }
        }
        return data;
    }

    @Override
    public DemuxerTrackMeta getMeta() {
        return other.getMeta();
    }

    @Override
    public boolean gotoFrame(long frameNo) throws IOException {
        return other.gotoFrame(frameNo);
    }

    @Override
    public boolean gotoSyncFrame(long frameNo) throws IOException {
        return other.gotoSyncFrame(frameNo);
    }

    @Override
    public long getCurFrame() {
        return other.getCurFrame();
    }

    @Override
    public void seek(double second) throws IOException {
        other.seek(second);
    }

    public SeekableDemuxerTrack getOther() {
        return other;
    }
}
