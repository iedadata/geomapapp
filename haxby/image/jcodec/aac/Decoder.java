package haxby.image.jcodec.aac;

import java.nio.ByteBuffer;

import haxby.image.jcodec.aac.filterbank.FilterBank;
import haxby.image.jcodec.aac.syntax.PCE;
import haxby.image.jcodec.aac.syntax.SyntacticElements;
import haxby.image.jcodec.aac.syntax.SyntaxConstants;
import haxby.image.jcodec.aac.transport.ADIFHeader;
import haxby.image.jcodec.common.AudioFormat;
import haxby.image.jcodec.common.io.BitReader;
import haxby.image.jcodec.common.logging.Logger;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 * 
 * Main AAC decoder class
 * 
 * @author in-somnia
 */
public class Decoder implements SyntaxConstants {
    private final AACDecoderConfig config;
    private final SyntacticElements syntacticElements;
    private final FilterBank filterBank;
    private ADIFHeader adifHeader;

    /**
     * The methods returns true, if a profile is supported by the decoder.
     * 
     * @param profile an AAC profile
     * @return true if the specified profile can be decoded
     * @see Profile#isDecodingSupported()
     */
    public static boolean canDecode(Profile profile) {
        return profile.isDecodingSupported();
    }

    /**
     * Initializes the decoder with a MP4 decoder specific info.
     *
     * After this the MP4 frames can be passed to the
     * <code>decodeFrame(byte[], SampleBuffer)</code> method to decode them.
     * 
     * @param decoderSpecificInfo a byte array containing the decoder specific info
     *                            from an MP4 container
     * @throws AACException if the specified profile is not supported
     */
    public Decoder(ByteBuffer decoderSpecificInfo) throws AACException {
        config = AACDecoderConfig.parseMP4DecoderSpecificInfo(decoderSpecificInfo);
        if (config == null)
            throw new IllegalArgumentException("illegal MP4 decoder specific info");

        if (!canDecode(config.getProfile()))
            throw new AACException("unsupported profile: " + config.getProfile().getDescription());

        syntacticElements = new SyntacticElements(config);
        filterBank = new FilterBank(config.isSmallFrameUsed(), config.getChannelConfiguration().getChannelCount());

        Logger.debug("profile: {0}", config.getProfile());
        Logger.debug("sf: {0}", config.getSampleFrequency().getFrequency());
        Logger.debug("channels: {0}", config.getChannelConfiguration().getDescription());
    }

    public AACDecoderConfig getConfig() {
        return config;
    }

    /**
     * Decodes one frame of AAC data in frame mode and returns the raw PCM data.
     * 
     * @param frame  the AAC frame
     * @param buffer a buffer to hold the decoded PCM data
     * @throws AACException if decoding fails
     */
    public ByteBuffer decodeFrame(ByteBuffer frame, ByteBuffer buffer) throws AACException {
        try {
            return decode(frame, buffer);
        } catch (AACException e) {
            if (!e.isEndOfStream())
                throw e;
            else
                Logger.warn("unexpected end of frame");
        }
        return null;
    }

    private ByteBuffer decode(ByteBuffer frame, ByteBuffer buffer) throws AACException {
        BitReader _in;
        if (ADIFHeader.isPresent(frame)) {
            int id = frame.getInt();
            _in = BitReader.createBitReader(frame);
            adifHeader = ADIFHeader.readHeader(_in);
            final PCE pce = adifHeader.getFirstPCE();
            config.setProfile(pce.getProfile());
            config.setSampleFrequency(pce.getSampleFrequency());
            config.setChannelConfiguration(ChannelConfiguration.forInt(pce.getChannelCount()));
        } else {
            _in = BitReader.createBitReader(frame);
        }

        if (!canDecode(config.getProfile()))
            throw new AACException("unsupported profile: " + config.getProfile().getDescription());

        syntacticElements.startNewFrame();

        try {
            // 1: bitstream parsing and noiseless coding
            syntacticElements.decode(_in);
            // 2: spectral processing
            syntacticElements.process(filterBank);
            // 3: send to output buffer
            return syntacticElements.sendToOutput(buffer);
        } catch (Exception e) {
            throw AACException.wrap(e);
        }
    }

    public AudioFormat getMeta() {
        return syntacticElements.getMeta();
    }
}
