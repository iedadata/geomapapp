package haxby.image.jcodec.codecs.h264.decode;

import static haxby.image.jcodec.codecs.h264.decode.MBlockDecoderUtils.collectPredictors;
import static haxby.image.jcodec.codecs.h264.decode.MBlockDecoderUtils.saveVectIntra;

import haxby.image.jcodec.codecs.h264.decode.aso.Mapper;
import haxby.image.jcodec.common.model.Picture;

/**
 * A decoder for Intra PCM macroblocks
 * 
 * @author The JCodec project
 */
public class MBlockDecoderIPCM {
    private Mapper mapper;
    private DecoderState s;

    public MBlockDecoderIPCM(Mapper mapper,  DecoderState decoderState) {
        this.mapper = mapper;
        this.s = decoderState;
    }

    public void decode(MBlock mBlock, Picture mb) {
        int mbX = mapper.getMbX(mBlock.mbIdx);
        collectPredictors(s, mb, mbX);
        saveVectIntra(s, mapper.getMbX(mBlock.mbIdx));
    }
}
