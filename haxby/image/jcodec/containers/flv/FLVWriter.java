package haxby.image.jcodec.containers.flv;
import java.io.IOException;
import java.nio.ByteBuffer;

import haxby.image.jcodec.common.io.NIOUtils;
import haxby.image.jcodec.common.io.SeekableByteChannel;
import haxby.image.jcodec.containers.flv.FLVTag.Type;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * FLV ( Flash Media Video ) muxer
 * 
 * @author Stan Vitvitskyy
 * 
 */
public class FLVWriter {
    // Write buffer, 1M
    private static final int WRITE_BUFFER_SIZE = 0x100000;

    private int startOfLastPacket = 9;
    private SeekableByteChannel out;
    private ByteBuffer writeBuf;

    public FLVWriter(SeekableByteChannel out) {
        this.out = out;
        writeBuf = ByteBuffer.allocate(WRITE_BUFFER_SIZE);
        writeHeader(writeBuf);
    }

    /**
     * Add a packet to the underlying file
     * 
     * @param pkt
     * @throws IOException
     */
    public void addPacket(FLVTag pkt) throws IOException {
        if (!writePacket(writeBuf, pkt)) {
            writeBuf.flip();
            startOfLastPacket -= out.write(writeBuf);
            writeBuf.clear();
            if (!writePacket(writeBuf, pkt))
                throw new RuntimeException("Unexpected");
        }
    }

    /**
     * Finish muxing and write the remaining data
     * 
     * @throws IOException
     */
    public void finish() throws IOException {
        writeBuf.flip();
        out.write(writeBuf);
    }

    private boolean writePacket(ByteBuffer writeBuf, FLVTag pkt) {
        int pktType = pkt.getType() == Type.VIDEO ? 0x9 : (pkt.getType() == Type.SCRIPT ? 0x12 : 0x8);
        int dataLen = pkt.getData().remaining();

        if (writeBuf.remaining() < 15 + dataLen)
            return false;

        writeBuf.putInt(writeBuf.position() - startOfLastPacket);
        startOfLastPacket = writeBuf.position();

        writeBuf.put((byte) pktType);
        writeBuf.putShort((short) (dataLen >> 8));
        writeBuf.put((byte) (dataLen & 0xff));

        writeBuf.putShort((short) ((pkt.getPts() >> 8) & 0xffff));
        writeBuf.put((byte) (pkt.getPts() & 0xff));
        writeBuf.put((byte) ((pkt.getPts() >> 24) & 0xff));

        writeBuf.putShort((short) 0);
        writeBuf.put((byte) 0);

        NIOUtils.write(writeBuf, pkt.getData().duplicate());

        return true;
    }

    private static void writeHeader(ByteBuffer writeBuf) {
        writeBuf.put((byte) 'F');
        writeBuf.put((byte) 'L');
        writeBuf.put((byte) 'V');
        writeBuf.put((byte) 1);
        writeBuf.put((byte) 5);
        writeBuf.putInt(9);
    }
}
