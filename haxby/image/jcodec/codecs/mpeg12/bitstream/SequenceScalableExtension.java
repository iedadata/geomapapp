package haxby.image.jcodec.codecs.mpeg12.bitstream;
import static haxby.image.jcodec.codecs.mpeg12.MPEGConst.IntraCoded;
import static haxby.image.jcodec.codecs.mpeg12.MPEGConst.PredictiveCoded;
import static haxby.image.jcodec.codecs.mpeg12.MPEGConst.mbTypeValB;
import static haxby.image.jcodec.codecs.mpeg12.MPEGConst.mbTypeValBSpat;
import static haxby.image.jcodec.codecs.mpeg12.MPEGConst.mbTypeValI;
import static haxby.image.jcodec.codecs.mpeg12.MPEGConst.mbTypeValISpat;
import static haxby.image.jcodec.codecs.mpeg12.MPEGConst.mbTypeValP;
import static haxby.image.jcodec.codecs.mpeg12.MPEGConst.mbTypeValPSpat;
import static haxby.image.jcodec.codecs.mpeg12.MPEGConst.vlcMBTypeB;
import static haxby.image.jcodec.codecs.mpeg12.MPEGConst.vlcMBTypeBSpat;
import static haxby.image.jcodec.codecs.mpeg12.MPEGConst.vlcMBTypeI;
import static haxby.image.jcodec.codecs.mpeg12.MPEGConst.vlcMBTypeISpat;
import static haxby.image.jcodec.codecs.mpeg12.MPEGConst.vlcMBTypeP;
import static haxby.image.jcodec.codecs.mpeg12.MPEGConst.vlcMBTypePSpat;
import static haxby.image.jcodec.codecs.mpeg12.MPEGConst.vlcMBTypeSNR;

import java.nio.ByteBuffer;

import haxby.image.jcodec.codecs.mpeg12.MPEGConst;
import haxby.image.jcodec.common.io.BitReader;
import haxby.image.jcodec.common.io.BitWriter;
import haxby.image.jcodec.common.io.VLC;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class SequenceScalableExtension implements MPEGHeader {

    public static final int DATA_PARTITIONING = 0;
    public static final int SPATIAL_SCALABILITY = 1;
    public static final int SNR_SCALABILITY = 2;
    public static final int TEMPORAL_SCALABILITY = 3;

    public int scalable_mode;
    public int layer_id;
    public int lower_layer_prediction_horizontal_size;
    public int lower_layer_prediction_vertical_size;
    public int horizontal_subsampling_factor_m;
    public int horizontal_subsampling_factor_n;
    public int vertical_subsampling_factor_m;
    public int vertical_subsampling_factor_n;
    public int picture_mux_enable;
    public int mux_to_progressive_sequence;
    public int picture_mux_order;
    public int picture_mux_factor;
    public static final int Sequence_Scalable_Extension = 0x5;

    public static SequenceScalableExtension read(BitReader _in) {
        SequenceScalableExtension sse = new SequenceScalableExtension();
        sse.scalable_mode = _in.readNBit(2);
        sse.layer_id = _in.readNBit(4);

        if (sse.scalable_mode == SequenceScalableExtension.SPATIAL_SCALABILITY) {
            sse.lower_layer_prediction_horizontal_size = _in.readNBit(14);
            _in.read1Bit();
            sse.lower_layer_prediction_vertical_size = _in.readNBit(14);
            sse.horizontal_subsampling_factor_m = _in.readNBit(5);
            sse.horizontal_subsampling_factor_n = _in.readNBit(5);
            sse.vertical_subsampling_factor_m = _in.readNBit(5);
            sse.vertical_subsampling_factor_n = _in.readNBit(5);
        }

        if (sse.scalable_mode == TEMPORAL_SCALABILITY) {
            sse.picture_mux_enable = _in.read1Bit();
            if (sse.picture_mux_enable != 0)
                sse.mux_to_progressive_sequence = _in.read1Bit();
            sse.picture_mux_order = _in.readNBit(3);
            sse.picture_mux_factor = _in.readNBit(3);
        }

        return sse;
    }

    @Override
    public void write(ByteBuffer bb) {
        BitWriter bw = new BitWriter(bb);
        bw.writeNBit(SequenceScalableExtension.Sequence_Scalable_Extension, 4);

        bw.writeNBit(scalable_mode, 2);
        bw.writeNBit(layer_id, 4);

        if (scalable_mode == SequenceScalableExtension.SPATIAL_SCALABILITY) {
            bw.writeNBit(lower_layer_prediction_horizontal_size, 14);
            bw.write1Bit(1); // todo: check this
            bw.writeNBit(lower_layer_prediction_vertical_size, 14);
            bw.writeNBit(horizontal_subsampling_factor_m, 5);
            bw.writeNBit(horizontal_subsampling_factor_n, 5);
            bw.writeNBit(vertical_subsampling_factor_m, 5);
            bw.writeNBit(vertical_subsampling_factor_n, 5);
        }

        if (scalable_mode == TEMPORAL_SCALABILITY) {
            bw.write1Bit(picture_mux_enable);
            if (picture_mux_enable != 0)
                bw.write1Bit(mux_to_progressive_sequence);
            bw.writeNBit(picture_mux_order, 3);
            bw.writeNBit(picture_mux_factor, 3);
        }
        bw.flush();
    }

    public static MPEGConst.MBType[] mbTypeVal(int picture_coding_type, SequenceScalableExtension sse) {
        if (sse != null && sse.scalable_mode == SNR_SCALABILITY) {
            return MPEGConst.mbTypeValSNR;
        } else if (sse != null && sse.scalable_mode == SPATIAL_SCALABILITY) {
            return picture_coding_type == IntraCoded ? mbTypeValISpat
                    : (picture_coding_type == PredictiveCoded ? mbTypeValPSpat : mbTypeValBSpat);
        } else {
            return picture_coding_type == IntraCoded ? mbTypeValI
                    : (picture_coding_type == PredictiveCoded ? mbTypeValP : mbTypeValB);
        }
    }

    public static VLC vlcMBType(int picture_coding_type, SequenceScalableExtension sse) {
        if (sse != null && sse.scalable_mode == SNR_SCALABILITY) {
            return vlcMBTypeSNR;
        } else if (sse != null && sse.scalable_mode == SPATIAL_SCALABILITY) {
            return picture_coding_type == IntraCoded ? vlcMBTypeISpat
                    : (picture_coding_type == PredictiveCoded ? vlcMBTypePSpat : vlcMBTypeBSpat);
        } else {
            return picture_coding_type == IntraCoded ? vlcMBTypeI
                    : (picture_coding_type == PredictiveCoded ? vlcMBTypeP : vlcMBTypeB);
        }
    }
}
