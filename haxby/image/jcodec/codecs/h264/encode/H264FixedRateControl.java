package haxby.image.jcodec.codecs.h264.encode;

import haxby.image.jcodec.codecs.h264.io.model.SliceType;
import haxby.image.jcodec.common.model.Picture;
import haxby.image.jcodec.common.model.Size;
import haxby.image.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * H.264 rate control policy that would produce frames of exactly equal size
 * 
 * @author The JCodec project
 * 
 */
public class H264FixedRateControl implements RateControl {
    private static final int INIT_QP = 26;
    private int balance;
    private int perMb;
    private int curQp;

    public H264FixedRateControl(int bitsPer256) {
        perMb = bitsPer256;
        curQp = INIT_QP;
    }

    @Override
    public int startPicture(Size sz, int maxSize, SliceType sliceType) {
        return INIT_QP + (sliceType == SliceType.P ? 4 : 0);
    }

    @Override
    public int accept(int bits) {

        balance += perMb - bits;

        return 0;
    }

    public void reset() {
        balance = 0;
        curQp = INIT_QP;
    }

    public int calcFrameSize(int nMB) {
        return ((256 + nMB * (perMb + 9)) >> 3) + (nMB >> 6);
    }

    public void setRate(int rate) {
        perMb = rate;
    }

    @Override
    public int initialQpDelta(Picture pic, int mbX, int mbY) {
        int qpDelta = balance < 0 ? (balance < -(perMb >> 1) ? 2 : 1)
                : (balance > perMb ? (balance > (perMb << 2) ? -2 : -1) : 0);
        int prevQp = curQp;
        curQp = MathUtil.clip(curQp + qpDelta, 12, 30);

        return curQp - prevQp;
    }
}
