package haxby.image.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

import haxby.image.jcodec.common.io.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class DataBox extends Box {
    private static final String FOURCC = "data";
    private int type;
    private int locale;
    private byte[] data;

    public DataBox(Header header) {
        super(header);
    }

    public static DataBox createDataBox(int type, int locale, byte[] data) {
        DataBox box = new DataBox(Header.createHeader(FOURCC, 0));
        box.type = type;
        box.locale = locale;
        box.data = data;
        return box;
    }

    @Override
    public void parse(ByteBuffer buf) {
        type = buf.getInt();
        locale = buf.getInt();
        data = NIOUtils.toArray(NIOUtils.readBuf(buf));
    }

    public int getType() {
        return type;
    }

    public int getLocale() {
        return locale;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    protected void doWrite(ByteBuffer out) {
        out.putInt(type);
        out.putInt(locale);
        out.put(data);
    }
    
    @Override
    public int estimateSize() {
        return 16 + data.length;
    }

    public static String fourcc() {
        return FOURCC;
    }
}
