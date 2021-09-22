package haxby.image.jcodec.aac.huffman;

import haxby.image.jcodec.aac.AACException;
import haxby.image.jcodec.common.io.BitReader;
import haxby.image.jcodec.common.io.VLC;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 *
 * @author in-somnia
 */
//TODO: implement decodeSpectralDataER
public class Huffman implements Codebooks {

    private static final boolean[] UNSIGNED = { false, false, true, true, false, false, true, true, true, true, true };
    private static final int QUAD_LEN = 4, PAIR_LEN = 2;

    private Huffman() {
    }

    private static void signValues(BitReader _in, int[] data, int off, int len) throws AACException {
        for (int i = off; i < off + len; i++) {
            if (data[i] != 0) {
                if (_in.readBool())
                    data[i] = -data[i];
            }
        }
    }

    private static int getEscape(BitReader _in, int s) throws AACException {
        final boolean neg = s < 0;

        int i = 4;
        while (_in.readBool()) {
            i++;
        }
        final int j = _in.readNBit(i) | (1 << i);

        return (neg ? -j : j);
    }

    public static int decodeScaleFactor(BitReader _in) throws AACException {
        final int offset = HCB_SF.readVLC(_in);
        return _HCB_SF[offset][2];
    }

    public static void decodeSpectralData(BitReader _in, int cb, int[] data, int off) throws AACException {
        final VLC HCB = CODEBOOKS[cb - 1];
        final int[][] _HCB = _CODEBOOKS[cb - 1];

        // find index
        final int offset = HCB.readVLC(_in);

        // copy data
        data[off] = _HCB[offset][2];
        data[off + 1] = _HCB[offset][3];
        if (cb < 5) {
            data[off + 2] = _HCB[offset][4];
            data[off + 3] = _HCB[offset][5];
        }

        // sign & escape
        if (cb < 11) {
            if (UNSIGNED[cb - 1])
                signValues(_in, data, off, cb < 5 ? QUAD_LEN : PAIR_LEN);
        } else if (cb == 11 || cb > 15) {
            signValues(_in, data, off, cb < 5 ? QUAD_LEN : PAIR_LEN); // virtual codebooks are always unsigned
            if (Math.abs(data[off]) == 16)
                data[off] = getEscape(_in, data[off]);
            if (Math.abs(data[off + 1]) == 16)
                data[off + 1] = getEscape(_in, data[off + 1]);
        } else
            throw new AACException("Huffman: unknown spectral codebook: " + cb);
    }
}
