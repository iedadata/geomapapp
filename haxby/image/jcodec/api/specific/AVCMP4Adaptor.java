package haxby.image.jcodec.api.specific;

import java.nio.ByteBuffer;

import haxby.image.jcodec.api.MediaInfo;
import haxby.image.jcodec.codecs.h264.H264Decoder;
import haxby.image.jcodec.codecs.h264.H264Utils;
import haxby.image.jcodec.codecs.h264.io.model.NALUnit;
import haxby.image.jcodec.codecs.h264.io.model.NALUnitType;
import haxby.image.jcodec.codecs.h264.io.model.SeqParameterSet;
import haxby.image.jcodec.common.DemuxerTrackMeta;
import haxby.image.jcodec.common.model.ColorSpace;
import haxby.image.jcodec.common.model.Packet;
import haxby.image.jcodec.common.model.Picture;
import haxby.image.jcodec.common.model.Rational;
import haxby.image.jcodec.common.model.Size;
import haxby.image.jcodec.containers.mp4.MP4Packet;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * High level frame grabber helper.
 * 
 * @author The JCodec project
 * 
 */
public class AVCMP4Adaptor implements ContainerAdaptor {

    private H264Decoder decoder;
    private int curENo;
    private Size size;
    private DemuxerTrackMeta meta;

    public AVCMP4Adaptor(DemuxerTrackMeta meta) {
        this.meta = meta;
        this.curENo = -1;

        calcBufferSize();
    }

    private void calcBufferSize() {
        int w = Integer.MIN_VALUE, h = Integer.MIN_VALUE;
        
        ByteBuffer bb = meta.getCodecPrivate().duplicate();
        ByteBuffer b;
        while((b = H264Utils.nextNALUnit(bb)) != null) {
            NALUnit nu = NALUnit.read(b);
            if(nu.type != NALUnitType.SPS)
                continue;
            SeqParameterSet sps = H264Utils.readSPS(b);
        
            int ww = sps.picWidthInMbsMinus1 + 1;
            if (ww > w)
                w = ww;
            int hh = SeqParameterSet.getPicHeightInMbs(sps);
            if (hh > h)
                h = hh;
        }

        size = new Size(w << 4, h << 4);
    }

    @Override
    public Picture decodeFrame(Packet packet, byte[][] data) {
        updateState(packet);

        Picture pic = decoder.decodeFrame(packet.getData(), data);
        Rational pasp = meta.getVideoCodecMeta().getPixelAspectRatio();

        if (pasp != null) {
            // TODO: transform
        }

        return pic;
    }
    
    private void updateState(Packet packet) {
        int eNo = ((MP4Packet) packet).getEntryNo();
        if (eNo != curENo) {
            curENo = eNo;
//            avcCBox = H264Utils.parseAVCC((VideoSampleEntry) ses[curENo]);
//            decoder = new H264Decoder();
//            ((H264Decoder) decoder).addSps(avcCBox.getSpsList());
//            ((H264Decoder) decoder).addPps(avcCBox.getPpsList());
        }
        if(decoder == null) {
            decoder = H264Decoder.createH264DecoderFromCodecPrivate(meta.getCodecPrivate());
        }
    }

    @Override
    public boolean canSeek(Packet pkt) {
        updateState(pkt);
        return H264Utils.idrSlice(H264Utils.splitFrame(pkt.getData()));
    }

    @Override
    public byte[][] allocatePicture() {
        return Picture.create(size.getWidth(), size.getHeight(), ColorSpace.YUV444).getData();
    }

    @Override
    public MediaInfo getMediaInfo() {
        return new MediaInfo(size);
    }
}