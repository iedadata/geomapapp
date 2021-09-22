package haxby.image.jcodec.scale.highbd;

import haxby.image.jcodec.common.model.PictureHiBD;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Performs color scaling from JPEG to NTSC
 * 
 * @author The JCodec project
 * 
 */
public class Yuv420jToYuv420HiBD implements TransformHiBD {
    public static int Y_COEFF = 7168;

    @Override
    public void transform(PictureHiBD src, PictureHiBD dst) {
        int[] sy = src.getPlaneData(0);
        int[] dy = dst.getPlaneData(0);
        for (int i = 0; i < src.getPlaneWidth(0) * src.getPlaneHeight(0); i++)
            dy[i] = (sy[i] * Y_COEFF >> 13) + 16;

        int[] su = src.getPlaneData(1);
        int[] du = dst.getPlaneData(1);
        for (int i = 0; i < src.getPlaneWidth(1) * src.getPlaneHeight(1); i++)
            du[i] = ((su[i] - 128) * Y_COEFF >> 13) + 128;

        int[] sv = src.getPlaneData(2);
        int[] dv = dst.getPlaneData(2);
        for (int i = 0; i < src.getPlaneWidth(2) * src.getPlaneHeight(2); i++)
            dv[i] = ((sv[i] - 128) * Y_COEFF >> 13) + 128;
    }
}
