package haxby.image.jcodec.containers.mkv.boxes;
import java.io.IOException;
import java.nio.ByteBuffer;

import haxby.image.jcodec.common.io.SeekableByteChannel;
import haxby.image.jcodec.containers.mkv.util.EbmlUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed under FreeBSD License
 * 
 * EBML IO implementation
 * 
 * @author The JCodec project
 * 
 */
public class EbmlBin extends EbmlBase {
    
    public ByteBuffer data;
    protected boolean dataRead = false;
    
    public EbmlBin(byte[] id) {
        super(id);
    }
    
    public void readChannel(SeekableByteChannel is) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate((int) this.dataLen);
        is.read(bb);
        bb.flip();
        this.read(bb);
    }

    public void read(ByteBuffer source) {
        this.data = source.slice();
        this.data.limit((int)dataLen);
        dataRead = true;
    }
    
    public void skip(ByteBuffer source) {
        if (!dataRead) {
            source.position((int)(dataOffset+dataLen));
            dataRead = true;
        }
    }
    
    public long size() {
        if (data == null || data.limit() == 0) 
            return super.size();
        
        long totalSize = data.limit();
        totalSize += EbmlUtil.ebmlLength(data.limit());
        totalSize += id.length;
        return totalSize; 
    }

    public void setBuf(ByteBuffer data) {
        this.data = data.slice();
        this.dataLen = this.data.limit();
    }
    
    public ByteBuffer getData() {
        int sizeSize = EbmlUtil.ebmlLength(data.limit());
        byte[] size = EbmlUtil.ebmlEncodeLen(data.limit(), sizeSize);
        
        ByteBuffer bb = ByteBuffer.allocate(id.length + sizeSize + data.limit());
        bb.put(id);
        bb.put(size);
        bb.put(data);

        bb.flip();
        data.flip();
        
        return bb;
    }

}
