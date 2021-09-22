package haxby.image.jcodec.codecs.h264.io.model;
import static haxby.image.jcodec.codecs.h264.decode.CAVLCReader.readBool;
import static haxby.image.jcodec.codecs.h264.decode.CAVLCReader.readU;
import static haxby.image.jcodec.codecs.h264.decode.CAVLCReader.readUEtrace;
import static haxby.image.jcodec.codecs.h264.io.write.CAVLCWriter.writeTrailingBits;

import java.nio.ByteBuffer;

import haxby.image.jcodec.common.io.BitReader;
import haxby.image.jcodec.common.io.BitWriter;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Sequence Parameter Set Extension entity of H264 bitstream
 * 
 * Capable to serialize / deserialize itself with CAVLC bit stream
 * 
 * @author The JCodec project
 * 
 */
public class SeqParameterSetExt {

    public int seq_parameter_set_id;
    public int aux_format_idc;
    public int bit_depth_aux_minus8;
    public boolean alpha_incr_flag;
    public boolean additional_extension_flag;
    public int alpha_opaque_value;
    public int alpha_transparent_value;

    public static SeqParameterSetExt read(ByteBuffer is) {
        BitReader _in = BitReader.createBitReader(is);

        SeqParameterSetExt spse = new SeqParameterSetExt();
        spse.seq_parameter_set_id = readUEtrace(_in, "SPSE: seq_parameter_set_id");
        spse.aux_format_idc = readUEtrace(_in, "SPSE: aux_format_idc");
        if (spse.aux_format_idc != 0) {
            spse.bit_depth_aux_minus8 = readUEtrace(_in, "SPSE: bit_depth_aux_minus8");
            spse.alpha_incr_flag = readBool(_in, "SPSE: alpha_incr_flag");
            spse.alpha_opaque_value = readU(_in, spse.bit_depth_aux_minus8 + 9, "SPSE: alpha_opaque_value");
            spse.alpha_transparent_value = readU(_in, spse.bit_depth_aux_minus8 + 9, "SPSE: alpha_transparent_value");
        }
        spse.additional_extension_flag = readBool(_in, "SPSE: additional_extension_flag");

        return spse;
    }

    public void write(ByteBuffer out) {
        BitWriter writer = new BitWriter(out);
        writeTrailingBits(writer);
    }
}