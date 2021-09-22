package haxby.image.jcodec.codecs.mjpeg;
import static haxby.image.jcodec.codecs.mjpeg.JpegConst.naturalOrder;

import java.nio.ByteBuffer;
import java.util.Arrays;

import haxby.image.jcodec.api.UnhandledStateException;
import haxby.image.jcodec.common.VideoCodecMeta;
import haxby.image.jcodec.common.VideoDecoder;
import haxby.image.jcodec.common.dct.SimpleIDCT10Bit;
import haxby.image.jcodec.common.io.BitReader;
import haxby.image.jcodec.common.io.NIOUtils;
import haxby.image.jcodec.common.io.VLC;
import haxby.image.jcodec.common.io.VLCBuilder;
import haxby.image.jcodec.common.logging.Logger;
import haxby.image.jcodec.common.model.ColorSpace;
import haxby.image.jcodec.common.model.Picture;
import haxby.image.jcodec.common.model.Rect;
import haxby.image.jcodec.common.model.Size;
import haxby.image.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class JpegDecoder extends VideoDecoder {
    private boolean interlace;
    private boolean topFieldFirst;
    int[] buf;

    public JpegDecoder() {
        this.buf = new int[64];
    }

    public void setInterlace(boolean interlace, boolean topFieldFirst) {
        this.interlace = interlace;
        this.topFieldFirst = topFieldFirst;
    }

    private Picture decodeScan(ByteBuffer data, FrameHeader header, ScanHeader scan, VLC[] huffTables,
            int[][] quant, byte[][] data2, int field, int step) {
        int blockW = header.getHmax();
        int blockH = header.getVmax();
        int mcuW = blockW << 3;
        int mcuH = blockH << 3;

        int width = header.width;
        int height = header.height;

        int xBlocks = (width + mcuW - 1) >> (blockW + 2);
        int yBlocks = (height + mcuH - 1) >> (blockH + 2);

        int nn = blockW + blockH;
        Picture result = new Picture(xBlocks << (blockW + 2), yBlocks << (blockH + 2), data2, null,
                nn == 4 ? ColorSpace.YUV420J : (nn == 3 ? ColorSpace.YUV422J : ColorSpace.YUV444J), 0, new Rect(0, 0,
                        width, height));

        BitReader bits = BitReader.createBitReader(data);
        int[] dcPredictor = new int[] { 1024, 1024, 1024 };
        for (int by = 0; by < yBlocks; by++)
            for (int bx = 0; bx < xBlocks && bits.moreData(); bx++)
                decodeMCU(bits, dcPredictor, quant, huffTables, result, bx, by, blockW, blockH, field, step);

        return result;
    }

    private static void putBlock(byte[] plane, int stride, int[] patch, int x, int y, int field, int step) {
        int dstride = step * stride;
        for (int i = 0, off = field * stride + y * dstride + x, poff = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++)
                plane[j + off] = (byte) (MathUtil.clip(patch[j + poff], 0, 255) - 128);
            off += dstride;
            poff += 8;
        }
    }

    private void decodeMCU(BitReader bits, int[] dcPredictor, int[][] quant, VLC[] huff, Picture result, int bx, int by,
                           int blockH, int blockV, int field, int step) {
        int sx = bx << (blockH - 1);
        int sy = by << (blockV - 1);

        for (int i = 0; i < blockV; i++)
            for (int j = 0; j < blockH; j++) {
                decodeBlock(bits, dcPredictor, quant, huff, result, buf, (sx + j) << 3, (sy + i) << 3, 0, 0, field,
                        step);
            }

        decodeBlock(bits, dcPredictor, quant, huff, result, buf, bx << 3, by << 3, 1, 1, field, step);
        decodeBlock(bits, dcPredictor, quant, huff, result, buf, bx << 3, by << 3, 2, 1, field, step);
    }

    void decodeBlock(BitReader bits, int[] dcPredictor, int[][] quant, VLC[] huff, Picture result, int[] buf,
            int blkX, int blkY, int plane, int chroma, int field, int step) {
        Arrays.fill(buf, 0);
        dcPredictor[plane] = buf[0] = readDCValue(bits, huff[chroma]) * quant[chroma][0] + dcPredictor[plane];
        readACValues(bits, buf, huff[chroma + 2], quant[chroma]);
        SimpleIDCT10Bit.idct10(buf, 0);

        putBlock(result.getPlaneData(plane), result.getPlaneWidth(plane), buf, blkX, blkY, field, step);
    }

    static int readDCValue(BitReader _in, VLC table) {
        int code = table.readVLC16(_in);
        return code != 0 ? toValue(_in.readNBit(code), code) : 0;
    }

    void readACValues(BitReader _in, int[] target, VLC table, int[] quantTable) {
        int code;
        int curOff = 1;
        do {
            code = table.readVLC16(_in);
            if (code == 0xF0) {
                curOff += 16;
            } else if (code > 0) {
                int rle = code >> 4;
                curOff += rle;
                int len = code & 0xf;
                target[naturalOrder[curOff]] = toValue(_in.readNBit(len), len) * quantTable[curOff];
                curOff++;
            }
        } while (code != 0 && curOff < 64);
    }

    static int toValue(int raw, int length) {
        return (length >= 1 && raw < (1 << length - 1)) ? -(1 << length) + 1 + raw : raw;
    }

    public Picture decodeFrame(ByteBuffer data, byte[][] data2) {

        if (interlace) {
            Picture r1 = decodeField(data, data2, topFieldFirst ? 0 : 1, 2);
            Picture r2 = decodeField(data, data2, topFieldFirst ? 1 : 0, 2);
            return Picture.createPicture(r1.getWidth(), r1.getHeight() << 1, data2, r1.getColor());
        } else {
            return decodeField(data, data2, 0, 1);
        }
    }

    public Picture decodeField(ByteBuffer data, byte[][] data2, int field, int step) {
        Picture result = null;

        FrameHeader header = null;
        VLC[] huffTables = new VLC[] { JpegConst.YDC_DEFAULT, JpegConst.CDC_DEFAULT, JpegConst.YAC_DEFAULT,
                JpegConst.CAC_DEFAULT };
        int[][] quant = new int[][] { JpegConst.DEFAULT_QUANT_LUMA, JpegConst.DEFAULT_QUANT_CHROMA };
        ScanHeader scan = null;
        boolean skipToNext = false;
        while (data.hasRemaining()) {
            int marker;
            if (!skipToNext) {
                marker = data.get() & 0xff;
            } else {
                while ((marker = (data.get() & 0xff)) != 0xff)
                    ;
            }
            skipToNext = false;
            if (marker == 0)
                continue;
            if (marker != 0xFF)
                throw new RuntimeException(
                        "@" + Long.toHexString(data.position()) + " Marker expected: 0x" + Integer.toHexString(marker));

            int b;
            while ((b = data.get() & 0xff) == 0xff)
                ;
            // Debug.trace("%s", JpegConst.toString(b));
            if (b == JpegConst.SOF0) {
                header = FrameHeader.read(data);
                // Debug.trace(" %s", image.frame);
            } else if (b == JpegConst.DHT) {
                int len1 = data.getShort() & 0xffff;
                ByteBuffer buf = NIOUtils.read(data, len1 - 2);
                while (buf.hasRemaining()) {
                    int tableNo = buf.get() & 0xff;
                    huffTables[(tableNo & 1) | ((tableNo >> 3) & 2)] = readHuffmanTable(buf);
                }
            } else if (b == JpegConst.DQT) {
                int len4 = data.getShort() & 0xffff;
                ByteBuffer buf = NIOUtils.read(data, len4 - 2);
                while (buf.hasRemaining()) {
                    int ind = buf.get() & 0xff;
                    quant[ind] = readQuantTable(buf);
                }
            } else if (b == JpegConst.SOS) {

                if (scan != null) {
                    throw new UnhandledStateException("unhandled - more than one scan header");
                }
                scan = ScanHeader.read(data);
                // Debug.trace(" %s", image.scan);
                result = decodeScan(readToMarker(data), header, scan, huffTables, quant, data2, field, step);
            } else if (b == JpegConst.SOI || (b >= JpegConst.RST0 && b <= JpegConst.RST7)) {
                Logger.warn("SOI not supported.");
                skipToNext = true;
            } else if (b == JpegConst.EOI) {
                break;
            } else if (b >= JpegConst.APP0 && b <= JpegConst.COM) {
                int len3 = data.getShort() & 0xffff;
                NIOUtils.read(data, len3 - 2);
            } else if (b == JpegConst.DRI) {
                Logger.warn("DRI not supported.");
                skipToNext = true;
            } else {
                if (b != 0)
                    Logger.warn("unhandled marker " + JpegConst.markerToString(b));
                skipToNext = true;
            }
        }

        return result;
    }

    private static ByteBuffer readToMarker(ByteBuffer data) {
        ByteBuffer out = ByteBuffer.allocate(data.remaining());
        while (data.hasRemaining()) {
            byte b0 = data.get();
            if (b0 == -1) {
                byte b1 = data.get();
                if (b1 == 0)
                    out.put((byte) -1);
                else {
                    data.position(data.position() - 2);
                    break;
                }
            } else
                out.put(b0);
        }
        out.flip();
        return out;
    }

    private static VLC readHuffmanTable(ByteBuffer data) {
        VLCBuilder builder = new VLCBuilder();

        byte[] levelSizes = NIOUtils.toArray(NIOUtils.read(data, 16));

        int levelStart = 0;
        for (int i = 0; i < 16; i++) {
            int length = levelSizes[i] & 0xff;
            for (int c = 0; c < length; c++) {
                int val = data.get() & 0xff;
                int code = levelStart++;
                builder.setInt(code, i + 1, val);
            }
            levelStart <<= 1;
        }

        return builder.getVLC();
    }

    private static int[] readQuantTable(ByteBuffer data) {
        int[] result = new int[64];
        for (int i = 0; i < 64; i++) {
            result[i] = data.get() & 0xff;
        }
        return result;
    }

    @Override
    public VideoCodecMeta getCodecMeta(ByteBuffer data) {
        FrameHeader header = null;
        while (data.hasRemaining()) {
            while (data.hasRemaining() && (data.get() & 0xff) != 0xff)
                continue;

            int type;
            while ((type = data.get() & 0xff) == 0xff)
                ;
            if (type == JpegConst.SOF0) {
                header = FrameHeader.read(data);
                break;
            }
        }
        if (header != null) {
            int blockW = header.getHmax();
            int blockH = header.getVmax();
            int nn = blockW + blockH;
            ColorSpace color = nn == 4 ? ColorSpace.YUV420J : (nn == 3 ? ColorSpace.YUV422J : ColorSpace.YUV444J);
            return haxby.image.jcodec.common.VideoCodecMeta.createSimpleVideoCodecMeta(new Size(header.width, header.height), color);
        }
        return null;
    }
}