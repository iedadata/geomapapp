package haxby.image.jcodec.aac.syntax;

import haxby.image.jcodec.aac.AACDecoderConfig;
import haxby.image.jcodec.aac.AACException;
import haxby.image.jcodec.aac.huffman.HCB;
import haxby.image.jcodec.aac.huffman.Huffman;
import haxby.image.jcodec.common.io.BitReader;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 *
 * @author in-somnia
 */
public class CCE extends Element implements SyntaxConstants {

    public static final int BEFORE_TNS = 0;
    public static final int AFTER_TNS = 1;
    public static final int AFTER_IMDCT = 2;
    private static final float[] CCE_SCALE = { 1.09050773266525765921f, 1.18920711500272106672f,
            1.4142135623730950488016887f, 2f };
    private final ICStream ics;
    private float[] iqData;
    private int couplingPoint;
    private int coupledCount;
    private final boolean[] channelPair;
    private final int[] idSelect;
    private final int[] chSelect;
    /*
     * [0] shared list of gains; [1] list of gains for right channel; [2] list of
     * gains for left channel; [3] lists of gains for both channels
     */
    private final float[][] gain;

    public CCE(int frameLength) {
        super();
        ics = new ICStream(frameLength);
        channelPair = new boolean[8];
        idSelect = new int[8];
        chSelect = new int[8];
        gain = new float[16][120];
    }

    int getCouplingPoint() {
        return couplingPoint;
    }

    int getCoupledCount() {
        return coupledCount;
    }

    boolean isChannelPair(int index) {
        return channelPair[index];
    }

    int getIDSelect(int index) {
        return idSelect[index];
    }

    int getCHSelect(int index) {
        return chSelect[index];
    }

    public void decode(BitReader _in, AACDecoderConfig conf) throws AACException {
        couplingPoint = 2 * _in.read1Bit();
        coupledCount = _in.readNBit(3);
        int gainCount = 0;
        int i;
        for (i = 0; i <= coupledCount; i++) {
            gainCount++;
            channelPair[i] = _in.readBool();
            idSelect[i] = _in.readNBit(4);
            if (channelPair[i]) {
                chSelect[i] = _in.readNBit(2);
                if (chSelect[i] == 3)
                    gainCount++;
            } else
                chSelect[i] = 2;
        }
        couplingPoint += _in.read1Bit();
        couplingPoint |= (couplingPoint >> 1);

        final boolean sign = _in.readBool();
        final double scale = CCE_SCALE[_in.readNBit(2)];

        ics.decode(_in, false, conf);
        final ICSInfo info = ics.getInfo();
        final int windowGroupCount = info.getWindowGroupCount();
        final int maxSFB = info.getMaxSFB();
        // TODO:
        final int[][] sfbCB = { {} };// ics.getSectionData().getSfbCB();

        for (i = 0; i < gainCount; i++) {
            int idx = 0;
            int cge = 1;
            int xg = 0;
            float gainCache = 1.0f;
            if (i > 0) {
                cge = couplingPoint == 2 ? 1 : _in.read1Bit();
                xg = cge == 0 ? 0 : Huffman.decodeScaleFactor(_in) - 60;
                gainCache = (float) Math.pow(scale, -xg);
            }
            if (couplingPoint == 2)
                gain[i][0] = gainCache;
            else {
                int sfb;
                for (int g = 0; g < windowGroupCount; g++) {
                    for (sfb = 0; sfb < maxSFB; sfb++, idx++) {
                        if (sfbCB[g][sfb] != HCB.ZERO_HCB) {
                            if (cge == 0) {
                                int t = Huffman.decodeScaleFactor(_in) - 60;
                                if (t != 0) {
                                    int s = 1;
                                    t = xg += t;
                                    if (!sign) {
                                        s -= 2 * (t & 0x1);
                                        t >>= 1;
                                    }
                                    gainCache = (float) (Math.pow(scale, -t) * s);
                                }
                            }
                            gain[i][idx] = gainCache;
                        }
                    }
                }
            }
        }
    }

    void process() throws AACException {
        iqData = ics.getInvQuantData();
    }

    void applyIndependentCoupling(int index, float[] data) {
        final double g = gain[index][0];
        for (int i = 0; i < data.length; i++) {
            data[i] += g * iqData[i];
        }
    }

    void applyDependentCoupling(int index, float[] data) {
        final ICSInfo info = ics.getInfo();
        final int[] swbOffsets = info.getSWBOffsets();
        final int windowGroupCount = info.getWindowGroupCount();
        final int maxSFB = info.getMaxSFB();
        // TODO:
        final int[][] sfbCB = { {} }; // ics.getSectionData().getSfbCB();

        int srcOff = 0;
        int dstOff = 0;

        int len, sfb, group, k, idx = 0;
        float x;
        for (int g = 0; g < windowGroupCount; g++) {
            len = info.getWindowGroupLength(g);
            for (sfb = 0; sfb < maxSFB; sfb++, idx++) {
                if (sfbCB[g][sfb] != HCB.ZERO_HCB) {
                    x = gain[index][idx];
                    for (group = 0; group < len; group++) {
                        for (k = swbOffsets[sfb]; k < swbOffsets[sfb + 1]; k++) {
                            data[dstOff + group * 128 + k] += x * iqData[srcOff + group * 128 + k];
                        }
                    }
                }
            }
            dstOff += len * 128;
            srcOff += len * 128;
        }
    }
}
