package haxby.image.jcodec.containers.mp4.boxes;

import static haxby.image.jcodec.common.JCodecUtil2.asciiString;

import java.nio.ByteBuffer;

import haxby.image.jcodec.common.io.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class FormatBox extends Box {
    private String fmt;

    public FormatBox(Header header) {
        super(header);
    }

    public static String fourcc() {
        return "frma";
    }

    public static FormatBox createFormatBox(String fmt) {
        FormatBox frma = new FormatBox(new Header(fourcc()));
        frma.fmt = fmt;
        return frma;
    }

    public void parse(ByteBuffer input) {
        this.fmt = NIOUtils.readString(input, 4);
    }

    protected void doWrite(ByteBuffer out) {
        out.put(asciiString(fmt));
    }
    
    @Override
    public int estimateSize() {
        return asciiString(fmt).length + 8;
    }
}