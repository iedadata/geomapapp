package haxby.image.jcodec.common.tools;
import static haxby.image.jcodec.common.Preconditions.checkState;
import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import haxby.image.jcodec.codecs.wav.WavHeader;
import haxby.image.jcodec.common.AudioFormat;
import haxby.image.jcodec.common.AudioUtil;
import haxby.image.jcodec.common.io.FileChannelWrapper;
import haxby.image.jcodec.common.io.NIOUtils;
import haxby.image.jcodec.common.io.SeekableByteChannel;
import haxby.image.jcodec.common.tools.MainUtils.Cmd;
import haxby.image.jcodec.common.tools.MainUtils.Flag;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Splits a multichannel wave file into a number of single-channel wavs
 * 
 * @author The JCodec project
 * 
 */
public class WavSplit {
    public static final Flag FLAG_PATTERN = Flag.flag("pattern", "p", "Output file name pattern, i.e. out%02d.wav");
    private static final Flag[] ALL_FLAGS = new Flag[] {FLAG_PATTERN};
    public static void main1(String[] args) throws Exception {
        Cmd cmd = MainUtils.parseArguments(args, ALL_FLAGS);
        if (cmd.argsLength() < 1) {
            MainUtils.printHelp(ALL_FLAGS, asList("filename.wav"));
            System.exit(-1);
        }

        File s = new File(args[0]);
        String pattern = cmd.getStringFlagD(FLAG_PATTERN, "c%02d.wav");

        WavHeader wavHeader = WavHeader.read(s);

        System.out.println("WAV: " + wavHeader.getFormat());

        checkState(2 == (int) wavHeader.fmt.numChannels);
        int dataOffset = wavHeader.dataOffset;
        FileChannelWrapper is = NIOUtils.readableChannel(s);
        is.setPosition(dataOffset);

        int channels = wavHeader.getFormat().getChannels();
        SeekableByteChannel[] out = new SeekableByteChannel[channels];
        for (int i = 0; i < channels; i++) {
            out[i] = NIOUtils.writableChannel((new File(s.getParentFile(), String.format(pattern, i))));
            WavHeader.copyWithChannels(wavHeader, 1).write(out[i]);
        }

        copy(wavHeader.getFormat(), is, out);

        for (int i = 0; i < channels; i++) {
            out[i].close();
        }
    }

    private static void copy(AudioFormat format, ReadableByteChannel is, SeekableByteChannel[] out) throws IOException {
        ByteBuffer[] outs = new ByteBuffer[out.length];
        for (int i = 0; i < out.length; i++) {
            outs[i] = ByteBuffer.allocate(format.framesToBytes(4096));
        }
        ByteBuffer inb = ByteBuffer.allocate(format.framesToBytes(4096) * out.length);

        while (is.read(inb) != -1) {
            inb.flip();
            AudioUtil.deinterleave(format, inb, outs);
            inb.clear();
            for (int i = 0; i < out.length; i++) {
                outs[i].flip();
                out[i].write(outs[i]);
                outs[i].clear();
            }
        }
    }
}
