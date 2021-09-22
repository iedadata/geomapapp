package haxby.image.jcodec.scale;
import java.lang.IllegalArgumentException;

import haxby.image.jcodec.common.model.ColorSpace;
import haxby.image.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class RgbToBgr implements Transform {

    @Override
    public void transform(Picture src, Picture dst) {
        if (src.getColor() != ColorSpace.RGB && src.getColor() != ColorSpace.BGR
                || dst.getColor() != ColorSpace.RGB && dst.getColor() != ColorSpace.BGR) {
            throw new IllegalArgumentException(
                    "Expected RGB or BGR inputs, was: " + src.getColor() + ", " + dst.getColor());
        }

        byte[] dataSrc = src.getPlaneData(0);
        byte[] dataDst = dst.getPlaneData(0);
        for (int i = 0; i < dataSrc.length; i += 3) {
            byte tmp = dataSrc[i + 2];
            dataDst[i + 2] = dataSrc[i];
            dataDst[i] = tmp;
            dataDst[i + 1] = dataSrc[i + 1];
        }
    }
}
