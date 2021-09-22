package haxby.image.jcodec.codecs.raw;
import static haxby.image.jcodec.common.model.ColorSpace.YUV422;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import haxby.image.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * The decoder for yuv 10 bit 422
 * 
 * x|x|9876543210(cr0)|9876543210(y0) |9876543210(cb0)
 * x|x|9876543210(y2) |9876543210(cb1)|9876543210(y1)
 * x|x|9876543210(cb2)|9876543210(y3) |9876543210(cr1)
 * x|x|9876543210(y5) |9876543210(cr2)|9876543210(y4) 
 * 
 * @author The JCodec project
 * 
 */
public class V210Decoder {

    private int width;
    private int height;

    public V210Decoder(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Picture decode(byte[] data) {
        ByteBuffer littleEndian = (ByteBuffer) ByteBuffer.wrap(data).order(LITTLE_ENDIAN);
        IntBuffer dat = littleEndian.asIntBuffer();
        ByteBuffer y = ByteBuffer.wrap(new byte[width * height]);
        ByteBuffer cb = ByteBuffer.wrap(new byte[width * height / 2]);
        ByteBuffer cr = ByteBuffer.wrap(new byte[width * height / 2]);

        while (dat.hasRemaining()) {
            int i = dat.get();
            cr.put(to8Bit(i >> 20));
            y.put(to8Bit((i >> 10) & 0x3ff));
            cb.put(to8Bit(i & 0x3ff));

            i = dat.get();
            y.put(to8Bit(i & 0x3ff));
            y.put(to8Bit(i >> 20));
            cb.put(to8Bit((i >> 10) & 0x3ff));

            i = dat.get();
            cb.put(to8Bit(i >> 20));
            y.put(to8Bit((i >> 10) & 0x3ff));
            cr.put(to8Bit(i & 0x3ff));

            i = dat.get();
            y.put(to8Bit(i & 0x3ff));
            y.put(to8Bit(i >> 20));
            cr.put(to8Bit((i >> 10) & 0x3ff));
        }

        return Picture.createPicture(width, height, new byte[][] { y.array(), cb.array(), cr.array() }, YUV422);
    }

    private byte to8Bit(int i) {
        return (byte)(((i + 2) >> 2) - 128);
    }
}