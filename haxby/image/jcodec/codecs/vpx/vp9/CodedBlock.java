package haxby.image.jcodec.codecs.vpx.vp9;

import haxby.image.jcodec.codecs.vpx.VPXBooleanDecoder;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class CodedBlock {
    public static final CodedBlock[] EMPTY_ARR = new CodedBlock[0];

    private ModeInfo mode;
    private Residual residual;

    public CodedBlock(ModeInfo mode, Residual r) {
        this.mode = mode;
        this.residual = r;
    }

    public ModeInfo getMode() {
        return mode;
    }

    public Residual getResidual() {
        return residual;
    }

    public static CodedBlock read(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder,
            DecodingContext c) {
        ModeInfo mode;
        if (c.isKeyIntraFrame())
            mode = new ModeInfo().read(miCol, miRow, blSz, decoder, c);
        else
            mode = new InterModeInfo().read(miCol, miRow, blSz, decoder, c);
        Residual r = Residual.readResidual(miCol, miRow, blSz, decoder, c, mode);
        return new CodedBlock(mode, r);
    }
}
