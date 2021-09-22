package haxby.image.jcodec.scale.highbd;
import static java.lang.System.arraycopy;

import haxby.image.jcodec.common.model.PictureHiBD;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Yuv444pToYuv420pHiBD implements TransformHiBD {
    private int shiftUp;
    private int shiftDown;

    public Yuv444pToYuv420pHiBD(int shiftUp, int shiftDown) {
        this.shiftUp = shiftUp;
        this.shiftDown = shiftDown;
    }

    public void transform(PictureHiBD src, PictureHiBD dst) {
        int lumaSize = src.getWidth() * src.getHeight();
        arraycopy(src.getPlaneData(0), 0, dst.getPlaneData(0), 0, lumaSize);
        copyAvg(src.getPlaneData(1), dst.getPlaneData(1), src.getPlaneWidth(1), src.getPlaneHeight(1));
        copyAvg(src.getPlaneData(2), dst.getPlaneData(2), src.getPlaneWidth(2), src.getPlaneHeight(2));

        if (shiftUp > shiftDown) {
            up(dst.getPlaneData(0), shiftUp - shiftDown);
            up(dst.getPlaneData(1), shiftUp - shiftDown);
            up(dst.getPlaneData(2), shiftUp - shiftDown);
        } else if (shiftDown > shiftUp) {
            down(dst.getPlaneData(0), shiftDown - shiftUp);
            down(dst.getPlaneData(1), shiftDown - shiftUp);
            down(dst.getPlaneData(2), shiftDown - shiftUp);
        }
    }

    private void down(int[] dst, int down) {
        for (int i = 0; i < dst.length; i++) {
            dst[i] >>= down;
        }
    }

    private void up(int[] dst, int up) {
        for (int i = 0; i < dst.length; i++) {
            dst[i] <<= up;
        }
    }

    private void copyAvg(int[] src, int[] dst, int width, int height) {
        int offSrc = 0, offDst = 0;
        for (int y = 0; y < (height >> 1); y++) {
            for (int x = 0; x < width; x += 2, offDst++, offSrc += 2) {
                dst[offDst] = (src[offSrc] + src[offSrc + 1] + src[offSrc + width] + src[offSrc + width + 1] + 2) >> 2;
            }
            offSrc += width;
        }
    }

}
