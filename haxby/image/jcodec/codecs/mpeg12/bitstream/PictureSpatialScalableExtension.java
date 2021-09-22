package haxby.image.jcodec.codecs.mpeg12.bitstream;
import java.nio.ByteBuffer;

import haxby.image.jcodec.common.io.BitReader;
import haxby.image.jcodec.common.io.BitWriter;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class PictureSpatialScalableExtension implements MPEGHeader {
    public int lower_layer_temporal_reference;
    public int lower_layer_horizontal_offset;
    public int lower_layer_vertical_offset;
    public int spatial_temporal_weight_code_table_index;
    public int lower_layer_progressive_frame;
    public int lower_layer_deinterlaced_field_select;
    public static final int Picture_Spatial_Scalable_Extension = 0x9;

    public static PictureSpatialScalableExtension read(BitReader _in) {
        PictureSpatialScalableExtension psse = new PictureSpatialScalableExtension();

        psse.lower_layer_temporal_reference = _in.readNBit(10);
        _in.read1Bit();
        psse.lower_layer_horizontal_offset = _in.readNBit(15);
        _in.read1Bit();
        psse.lower_layer_vertical_offset = _in.readNBit(15);
        psse.spatial_temporal_weight_code_table_index = _in.readNBit(2);
        psse.lower_layer_progressive_frame = _in.read1Bit();
        psse.lower_layer_deinterlaced_field_select = _in.read1Bit();

        return psse;
    }

    @Override
    public void write(ByteBuffer bb) {
        BitWriter bw = new BitWriter(bb);
        bw.writeNBit(PictureSpatialScalableExtension.Picture_Spatial_Scalable_Extension, 4);

        bw.writeNBit(lower_layer_temporal_reference, 10);
        bw.write1Bit(1); // todo: verify this
        bw.writeNBit(lower_layer_horizontal_offset, 15);
        bw.write1Bit(1); // todo: verify this
        bw.writeNBit(lower_layer_vertical_offset, 15);
        bw.writeNBit(spatial_temporal_weight_code_table_index, 2);
        bw.write1Bit(lower_layer_progressive_frame);
        bw.write1Bit(lower_layer_deinterlaced_field_select);

        bw.flush();
    }
}
