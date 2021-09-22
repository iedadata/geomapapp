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
public class QuantMatrixExtension implements MPEGHeader {

    public int[] intra_quantiser_matrix;
    public int[] non_intra_quantiser_matrix;
    public int[] chroma_intra_quantiser_matrix;
    public int[] chroma_non_intra_quantiser_matrix;
    public static final int Quant_Matrix_Extension = 0x3;

    public static QuantMatrixExtension read(BitReader _in) {
        QuantMatrixExtension qme = new QuantMatrixExtension();
        if (_in.read1Bit() != 0)
            qme.intra_quantiser_matrix = readQMat(_in);
        if (_in.read1Bit() != 0)
            qme.non_intra_quantiser_matrix = readQMat(_in);
        if (_in.read1Bit() != 0)
            qme.chroma_intra_quantiser_matrix = readQMat(_in);
        if (_in.read1Bit() != 0)
            qme.chroma_non_intra_quantiser_matrix = readQMat(_in);

        return qme;
    }

    private static int[] readQMat(BitReader _in) {
        int[] qmat = new int[64];
        for (int i = 0; i < 64; i++)
            qmat[i] = _in.readNBit(8);
        return qmat;
    }

    @Override
    public void write(ByteBuffer bb) {
        BitWriter bw = new BitWriter(bb);
        bw.writeNBit(QuantMatrixExtension.Quant_Matrix_Extension, 4);
        
        bw.write1Bit(intra_quantiser_matrix != null ? 1 : 0);
        if (intra_quantiser_matrix != null)
            writeQMat(intra_quantiser_matrix, bw);
        bw.write1Bit(non_intra_quantiser_matrix != null ? 1 : 0);
        if (non_intra_quantiser_matrix != null)
            writeQMat(non_intra_quantiser_matrix, bw);
        bw.write1Bit(chroma_intra_quantiser_matrix != null ? 1 : 0);
        if (chroma_intra_quantiser_matrix != null)
            writeQMat(chroma_intra_quantiser_matrix, bw);
        bw.write1Bit(chroma_non_intra_quantiser_matrix != null ? 1 : 0);
        if (chroma_non_intra_quantiser_matrix != null)
            writeQMat(chroma_non_intra_quantiser_matrix, bw);
        
        bw.flush();
    }

    private void writeQMat(int[] matrix, BitWriter ob) {
        for (int i = 0; i < 64; i++)
            ob.writeNBit(matrix[i], 8);
    }
}