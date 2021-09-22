package haxby.image.jcodec.codecs.png;

import static haxby.image.jcodec.codecs.png.IHDR.PNG_COLOR_MASK_COLOR;
import static haxby.image.jcodec.codecs.png.PNGConsts.TAG_IDAT;
import static haxby.image.jcodec.codecs.png.PNGConsts.TAG_IEND;
import static haxby.image.jcodec.codecs.png.PNGConsts.TAG_IHDR;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import haxby.image.jcodec.common.VideoEncoder;
import haxby.image.jcodec.common.io.NIOUtils;
import haxby.image.jcodec.common.model.ColorSpace;
import haxby.image.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Simplistic PNG encoder, doesn't support anything.
 * 
 * @author Stanislav Vitvitskyy
 * 
 */
public class PNGEncoder extends VideoEncoder {
    private static int crc32(ByteBuffer from, ByteBuffer to) {
        from.limit(to.position());
        
        CRC32 crc32 = new CRC32();
        crc32.update(NIOUtils.toArray(from));
        return (int) crc32.getValue();
    }

    @Override
    public EncodedFrame encodeFrame(Picture pic, ByteBuffer out) {
        ByteBuffer _out = out.duplicate();
        _out.putInt(PNGConsts.PNGSIGhi);
        _out.putInt(PNGConsts.PNGSIGlo);
        IHDR ihdr = new IHDR();
        ihdr.width = pic.getCroppedWidth();
        ihdr.height = pic.getCroppedHeight();
        ihdr.bitDepth = 8;
        ihdr.colorType = PNG_COLOR_MASK_COLOR;
        _out.putInt(13);
        
        ByteBuffer crcFrom = _out.duplicate();
        _out.putInt(TAG_IHDR);
        ihdr.write(_out);
        _out.putInt(crc32(crcFrom, _out));
        
        Deflater deflater = new Deflater();
        byte[] rowData = new byte[pic.getCroppedWidth() * 3 + 1];
        byte[] pix = pic.getPlaneData(0);
        byte[] buffer = new byte[1 << 15];
        int ptr = 0, len = buffer.length;

        // We do one extra iteration here to flush the deflator
        int lineStep = (pic.getWidth() - pic.getCroppedWidth()) * 3;
        for (int row = 0, bptr = 0; row < pic.getCroppedHeight() + 1; row++) {
            int count;
            while ((count = deflater.deflate(buffer, ptr, len)) > 0) {
                ptr += count;
                len -= count;

                if (len == 0) {
                    _out.putInt(ptr);
                    crcFrom = _out.duplicate();
                    _out.putInt(TAG_IDAT);
                    _out.put(buffer, 0, ptr);
                    _out.putInt(crc32(crcFrom, _out));
                    ptr = 0;
                    len = buffer.length;
                }
            }

            if (row >= pic.getCroppedHeight())
                break;

            rowData[0] = 0; // no filter
            for (int i = 1; i <= pic.getCroppedWidth() * 3; i += 3, bptr += 3) {
                rowData[i] = (byte) (pix[bptr] + 128);
                rowData[i + 1] = (byte) (pix[bptr + 1] + 128);
                rowData[i + 2] = (byte) (pix[bptr + 2] + 128);
            }
            bptr += lineStep;
            deflater.setInput(rowData);
            if (row >= pic.getCroppedHeight() - 1)
                deflater.finish();
        }
        if (ptr > 0) {
            _out.putInt(ptr);
            crcFrom = _out.duplicate();
            _out.putInt(TAG_IDAT);
            _out.put(buffer, 0, ptr);
            _out.putInt(crc32(crcFrom, _out));
        }
        _out.putInt(0);
        _out.putInt(TAG_IEND);
        _out.putInt(0xae426082);
        _out.flip();

        return new EncodedFrame(_out, true);
    }

    @Override
    public ColorSpace[] getSupportedColorSpaces() {
        return new ColorSpace[] { ColorSpace.RGB };
    }

    @Override
    public int estimateBufferSize(Picture frame) {
        return frame.getCroppedWidth() * frame.getCroppedHeight() * 4;
    }

    @Override
    public void finish() {
        // TODO Auto-generated method stub
        
    }
}
