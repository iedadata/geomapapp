package haxby.image.jcodec.codecs.h264.encode;

import haxby.image.jcodec.codecs.h264.io.model.MBType;
import haxby.image.jcodec.common.model.ColorSpace;
import haxby.image.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
public class EncodedMB {
    public Picture pixels;
    public MBType type;
    public int qp;
    public int[] nc;
    public int[] mx;
    public int[] my;
    public int[] mr;
    public int mbX;
    public int mbY;

    public EncodedMB() {
        pixels = Picture.create(16, 16, ColorSpace.YUV420J);
        nc = new int[16];
        mx = new int[16];
        my = new int[16];
        mr = new int[16];
    }

    public Picture getPixels() {
        return pixels;
    }

    public MBType getType() {
        return type;
    }

    public void setType(MBType type) {
        this.type = type;
    }

    public int getQp() {
        return qp;
    }

    public void setQp(int qp) {
        this.qp = qp;
    }

    public int[] getNc() {
        return nc;
    }

    public int[] getMx() {
        return mx;
    }

    public int[] getMy() {
        return my;
    }

    public void setPos(int mbX, int mbY) {
        this.mbX  = mbX;
        this.mbY = mbY;
    }

    public int[] getMr() {
        return mr;
    }
}
