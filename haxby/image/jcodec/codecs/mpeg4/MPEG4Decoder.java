package haxby.image.jcodec.codecs.mpeg4;

import static haxby.image.jcodec.codecs.mpeg4.MPEG4BiRenderer.renderBi;
import static haxby.image.jcodec.codecs.mpeg4.MPEG4Bitstream.checkResyncMarker;
import static haxby.image.jcodec.codecs.mpeg4.MPEG4Bitstream.readBi;
import static haxby.image.jcodec.codecs.mpeg4.MPEG4Bitstream.readCoeffIntra;
import static haxby.image.jcodec.codecs.mpeg4.MPEG4Bitstream.readInterCoeffs;
import static haxby.image.jcodec.codecs.mpeg4.MPEG4Bitstream.readInterModeCoeffs;
import static haxby.image.jcodec.codecs.mpeg4.MPEG4Bitstream.readIntraMode;
import static haxby.image.jcodec.codecs.mpeg4.MPEG4Bitstream.readVideoPacketHeader;
import static haxby.image.jcodec.codecs.mpeg4.MPEG4Renderer.renderInter;
import static haxby.image.jcodec.codecs.mpeg4.MPEG4Renderer.renderIntra;

import java.nio.ByteBuffer;

import haxby.image.jcodec.codecs.mpeg4.Macroblock.Vector;
import haxby.image.jcodec.common.UsedViaReflection;
import haxby.image.jcodec.common.VideoCodecMeta;
import haxby.image.jcodec.common.VideoDecoder;
import haxby.image.jcodec.common.io.BitReader;
import haxby.image.jcodec.common.model.ColorSpace;
import haxby.image.jcodec.common.model.Picture;
import haxby.image.jcodec.common.model.Rect;
import haxby.image.jcodec.common.model.Size;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MPEG4Decoder extends VideoDecoder {
    private Picture[] refs;

    private Macroblock[] prevMBs;
    private Macroblock[] mbs;

    private MPEG4DecodingContext ctx;

    public MPEG4Decoder() {
        refs = new Picture[2];
    }

    @Override
    public Picture decodeFrame(ByteBuffer data, byte[][] buffer) {
        if (ctx == null)
            ctx = new MPEG4DecodingContext();
        if (!ctx.readHeaders(data))
            return null;

        ctx.fcodeForward = ctx.fcodeBackward = ctx.intraDCThreshold = 0;

        BitReader br = BitReader.createBitReader(data);

        if (!ctx.readVOPHeader(br))
            return null;

        mbs = new Macroblock[ctx.mbWidth * ctx.mbHeight];
        for (int i = 0; i < mbs.length; i++) {
            mbs[i] = new Macroblock();
        }

        Picture decoded = null;
        if (ctx.codingType != MPEG4Bitstream.B_VOP) {
            switch (ctx.codingType) {
            case MPEG4Bitstream.I_VOP:
                decoded = decodeIFrame(br, ctx, buffer);
                break;
            case MPEG4Bitstream.P_VOP:
                decoded = decodePFrame(br, ctx, buffer, ctx.fcodeForward);
                break;
            case MPEG4Bitstream.S_VOP:
                throw new RuntimeException("GMC not supported.");
            case MPEG4Bitstream.N_VOP:
                return null;
            }

            refs[1] = refs[0];
            refs[0] = decoded;

            prevMBs = mbs;
        } else {
            decoded = decodeBFrame(br, ctx, buffer, ctx.quant, ctx.fcodeForward, ctx.fcodeBackward);
        }
        // We don't want to overread
        br.terminate();

        return decoded;
    }

    private Picture decodeIFrame(BitReader br, MPEG4DecodingContext ctx, byte[][] buffer) {
        Picture p = new Picture(ctx.mbWidth << 4, ctx.mbHeight << 4, buffer, null, ColorSpace.YUV420, 0,
                new Rect(0, 0, ctx.width, ctx.height));
        int bound = 0;

        for (int y = 0; y < ctx.mbHeight; y++) {
            for (int x = 0; x < ctx.mbWidth; x++) {
                Macroblock mb = mbs[y * ctx.mbWidth + x];
                mb.reset(x, y, bound);

                readIntraMode(br, ctx, mb);
                int index = x + y * ctx.mbWidth;
                Macroblock aboveMb = null;
                Macroblock aboveLeftMb = null;
                Macroblock leftMb = null;
                int apos = index - ctx.mbWidth;
                int lpos = index - 1;
                int alpos = index - 1 - ctx.mbWidth;
                if (apos >= bound)
                    aboveMb = mbs[apos];
                if (alpos >= bound)
                    aboveLeftMb = mbs[alpos];
                if (x > 0 && lpos >= bound)
                    leftMb = mbs[lpos];
                readCoeffIntra(br, ctx, mb, aboveMb, leftMb, aboveLeftMb);
                
                x = mb.x;
                y = mb.y;
                bound = mb.bound;

                renderIntra(mb, ctx);
                
                putPix(p, mb, x, y);
            }
        }

        return p;
    }

    Picture decodePFrame(BitReader br, MPEG4DecodingContext ctx, byte[][] buffers, int fcode) {
        int bound = 0;
        int mbWidth = ctx.mbWidth;
        int mbHeight = ctx.mbHeight;

        Picture p = new Picture(ctx.mbWidth << 4, ctx.mbHeight << 4, buffers, null, ColorSpace.YUV420, 0,
                new Rect(0, 0, ctx.width, ctx.height));

        for (int y = 0; y < mbHeight; y++) {
            for (int x = 0; x < mbWidth; x++) {
                // skip stuffing
                while (br.checkNBit(10) == 1)
                    br.skip(10);

                if (checkResyncMarker(br, fcode - 1)) {
                    bound = readVideoPacketHeader(br, ctx, fcode - 1, true, false, true);
                    x = bound % mbWidth;
                    y = bound / mbWidth;
                }

                int index = x + y * ctx.mbWidth;
                Macroblock aboveMb = null;
                Macroblock aboveLeftMb = null;
                Macroblock leftMb = null;
                Macroblock aboveRightMb = null;
                int apos = index - ctx.mbWidth;
                int lpos = index - 1;
                int alpos = index - 1 - ctx.mbWidth;
                int arpos = index + 1 - ctx.mbWidth;
                if (apos >= bound)
                    aboveMb = mbs[apos];
                if (alpos >= bound)
                    aboveLeftMb = mbs[alpos];
                if (x > 0 && lpos >= bound)
                    leftMb = mbs[lpos];
                if (arpos >= bound && x < ctx.mbWidth - 1)
                    aboveRightMb = mbs[arpos];
                
                Macroblock mb = mbs[y * ctx.mbWidth + x];

                mb.reset(x, y, bound);

                readInterModeCoeffs(br, ctx, fcode, mb, aboveMb, leftMb, aboveLeftMb, aboveRightMb);
                
                renderInter(ctx, refs, mb, fcode, 0, false);
                
                putPix(p, mb, x, y);
            }
        }

        return p;
    }

    private Picture decodeBFrame(BitReader br, MPEG4DecodingContext ctx, byte[][] buffers, int quant, final int fcodeForward,
            final int fcodeBackward) {

        Picture p = new Picture(ctx.mbWidth << 4, ctx.mbHeight << 4, buffers, null, ColorSpace.YUV420, 0,
                new Rect(0, 0, ctx.width, ctx.height));

        Vector pFMV = Macroblock.vec(), pBMV = Macroblock.vec();
        //To prevent unexpected behaviour in Javascript, final variables must be declared at method level and not inside loops
        final int fcodeMax = (fcodeForward > fcodeBackward) ? fcodeForward : fcodeBackward;
        for (int y = 0; y < ctx.mbHeight; y++) {
            pBMV.x = pBMV.y = pFMV.x = pFMV.y = 0;

            for (int x = 0; x < ctx.mbWidth; x++) {
                Macroblock mb = mbs[y * ctx.mbWidth + x];
                Macroblock lastMB = prevMBs[y * ctx.mbWidth + x];

                if (checkResyncMarker(br, fcodeMax - 1)) {
                    int bound = readVideoPacketHeader(br, ctx, fcodeMax - 1, fcodeForward != 0, fcodeBackward != 0,
                            ctx.intraDCThreshold != 0);
                    x = bound % ctx.mbWidth;
                    y = bound / ctx.mbWidth;
                    pBMV.x = pBMV.y = pFMV.x = pFMV.y = 0;
                }
                mb.x = x;
                mb.y = y;

                mb.quant = quant;

                if (lastMB.mode == MPEG4Consts.MODE_NOT_CODED) {
                    mb.cbp = 0;
                    mb.mode = MPEG4Consts.MODE_FORWARD;
                    readInterCoeffs(br, ctx, mb);
                    renderInter(ctx, refs, lastMB, fcodeForward, 1, true);
                    putPix(p, lastMB, x, y);
                    continue;
                }

                readBi(br, ctx, fcodeForward, fcodeBackward, mb, lastMB, pFMV, pBMV);
                renderBi(ctx, refs, fcodeForward, fcodeBackward, mb);
                
                putPix(p, mb, x, y);
            }
        }

        return p;
    }
    
    public static void putPix(Picture p, Macroblock mb, int x, int y) {
        byte[] plane0 = p.getPlaneData(0);
        int dsto0 = ((y << 4)) * p.getWidth() + (x << 4);
        for (int row = 0, srco = 0; row < 16; row++, dsto0 += p.getWidth()) {
            for (int col = 0; col < 16; col++, srco++) {
                plane0[dsto0 + col] = mb.pred[0][srco];
            }
        }
        for (int pl = 1; pl < 3; pl++) {
            byte[] plane = p.getPlaneData(pl);
            int dsto = ((y << 3)) * p.getPlaneWidth(pl) + (x << 3);
            for (int row = 0, srco = 0; row < 8; row++, dsto += p.getPlaneWidth(pl)) {
                for (int col = 0; col < 8; col++, srco++) {
                    plane[dsto + col] = mb.pred[pl][srco];
                }
            }
        }
    }

    @Override
    public VideoCodecMeta getCodecMeta(ByteBuffer data) {
        MPEG4DecodingContext ctx = MPEG4DecodingContext.readFromHeaders(data.duplicate());
        if (ctx == null)
            return null;
        
        return VideoCodecMeta.createSimpleVideoCodecMeta(new Size(ctx.width, ctx.height), ColorSpace.YUV420J);
    }

    @UsedViaReflection
    public static int probe(ByteBuffer data) {
        MPEG4DecodingContext ctx = MPEG4DecodingContext.readFromHeaders(data.duplicate());
        if (ctx == null)
            return 0;
        return Math.min(ctx.width > 320 ? (ctx.width < 1280 ? 100 : 50) : 50,
                ctx.height > 240 ? (ctx.height < 720 ? 100 : 50) : 50);
    }
}