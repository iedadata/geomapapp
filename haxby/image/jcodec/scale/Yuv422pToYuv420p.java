package haxby.image.jcodec.scale;

import static java.lang.System.arraycopy;

import haxby.image.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Yuv422pToYuv420p implements Transform {

    @Override
    public void transform(Picture src, Picture dst) {
        int lumaSize = src.getWidth() * src.getHeight();
        arraycopy(src.getPlaneData(0), 0, dst.getPlaneData(0), 0, lumaSize);
        copyAvg(src.getPlaneData(1), dst.getPlaneData(1), src.getPlaneWidth(1), src.getPlaneHeight(1));
        copyAvg(src.getPlaneData(2), dst.getPlaneData(2), src.getPlaneWidth(2), src.getPlaneHeight(2));
    }

    private void copyAvg(byte[] src, byte[] dst, int width, int height) {
        int offSrc = 0, offDst = 0;
        for (int y = 0; y < height / 2; y++) {
            for (int x = 0; x < width; x++, offDst++, offSrc++) {
                dst[offDst] = (byte) ((src[offSrc] + src[offSrc + width] + 1) >> 1);
            }
            offSrc += width;
        }
    }
}
