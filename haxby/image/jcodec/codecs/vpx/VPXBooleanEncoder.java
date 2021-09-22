package haxby.image.jcodec.codecs.vpx;
import java.nio.ByteBuffer;

import haxby.image.jcodec.codecs.vpx.VPXConst;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Containes boolean encoder from VPx codecs
 * 
 * @author The JCodec project
 * 
 */
public class VPXBooleanEncoder {
    private ByteBuffer out;
    private int lowvalue;
    private int range;
    private int count;

    public VPXBooleanEncoder(ByteBuffer out) {
        this.out = out;
        lowvalue = 0;
        range = 255;
        count = -24;
    }

    public void writeBit(int prob, int bb) {
        int split = 1 + (((range - 1) * prob) >> 8);

        if (bb != 0) {
            lowvalue += split;
            range -= split;
        } else {
            range = split;
        }

        int shift = VPXConst.vp8Norm[range];
        range <<= shift;
        count += shift;

        if (count >= 0) {
            int offset = shift - count;

            if (((lowvalue << (offset - 1)) & 0x80000000) != 0) {
                int x = out.position() - 1;

                while (x >= 0 && out.get(x) == -1) {
                    out.put(x, (byte) 0);
                    x--;
                }

                out.put(x, (byte) ((out.get(x) & 0xff) + 1));
            }

            out.put((byte) (lowvalue >> (24 - offset)));
            lowvalue <<= offset;
            shift = count;
            lowvalue &= 0xffffff;
            count -= 8;
        }

        lowvalue <<= shift;
    }

    public void stop() {
        int i;

        for (i = 0; i < 32; i++)
            writeBit(128, 0);
    }

    public int position() {
        return out.position() + ((count + 24) >> 3);
    }
}