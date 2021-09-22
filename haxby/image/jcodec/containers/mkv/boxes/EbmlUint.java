package haxby.image.jcodec.containers.mkv.boxes;
import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed under FreeBSD License
 * 
 * EBML IO implementation
 * 
 * @author The JCodec project
 * 
 */
public class EbmlUint extends EbmlBin {

    public EbmlUint(byte[] id) {
        super(id);
    }
    
    public static EbmlUint createEbmlUint(byte[] id, long value) {
        EbmlUint e = new EbmlUint(id);
        e.setUint(value);
        return e;
    }
    
    public void setUint(long value){
        this.data = ByteBuffer.wrap(longToBytes(value));
        this.dataLen = this.data.limit();
    }

    public long getUint() {
        long l = 0;
        long tmp = 0;
        for (int i = 0; i < data.limit(); i++) {
            tmp = ((long) data.get(data.limit() - 1 - i)) << 56;
            tmp >>>= (56 - (i * 8));
            l |= tmp;
        }
        return l;
    }
    
    public static byte[] longToBytes(long value) {
        byte[] b = new byte[calculatePayloadSize(value)];
        for (int i = b.length - 1; i >= 0; i--) {
            b[i] = (byte) (value >>> (8 * (b.length - i - 1)));
        }
        return b;
    }

    public static int calculatePayloadSize(long value) {
        if (value == 0)
            return 1;
        if (value <= 0x7fffffffL) {
            return 4 - (Integer.numberOfLeadingZeros((int) value) >> 3);
        }
        return 8 - (Long.numberOfLeadingZeros(value) >> 3);
    }
}
