package haxby.image.jcodec.containers.mkv.boxes;
import static haxby.image.jcodec.containers.mkv.util.EbmlUtil.ebmlEncode;
import static haxby.image.jcodec.containers.mkv.util.EbmlUtil.ebmlLength;
import static haxby.image.jcodec.platform.Platform.arrayEqualsByte;

import java.lang.System;
import java.nio.ByteBuffer;

import haxby.image.jcodec.containers.mkv.util.EbmlUtil;
import haxby.image.jcodec.platform.Platform;


/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * EBML IO implementation
 * 
 * @author The JCodec project
 * 
 */
public class MkvSegment extends EbmlMaster {
    
    int headerSize = 0;
    public static final byte[] SEGMENT_ID = new byte[]{0x18, 0x53, (byte)0x80, 0x67};

    public MkvSegment(byte[] id) {
        super(id);
    }
    
    public static MkvSegment createMkvSegment() {
        return new MkvSegment(SEGMENT_ID);
    }
 
    public ByteBuffer getHeader() {
        long headerSize = getHeaderSize();
        
        if (headerSize > Integer.MAX_VALUE)
            System.out.println("MkvSegment.getHeader: id.length "+id.length+"  Element.getEbmlSize("+dataLen+"): "+EbmlUtil.ebmlLength(dataLen)+" size: "+dataLen);
        ByteBuffer bb = ByteBuffer.allocate((int)headerSize);

        bb.put(id);
        bb.put(ebmlEncode(getDataLen()));

        if (children != null && !children.isEmpty()){
            // all non-cluster elements go to header
            for(EbmlBase e : children){
                if (arrayEqualsByte(CLUSTER_ID, e.type.id))
                    continue;
                
                bb.put(e.getData()); 
            }
        }
        
        bb.flip();
        
        return bb;
    }
    
    public long getHeaderSize(){
        long returnValue = id.length;
        returnValue += ebmlLength(getDataLen());
        if (children != null && !children.isEmpty()){
            for(EbmlBase e : children){
                if (arrayEqualsByte(CLUSTER_ID, e.type.id))
                    continue;
                
                returnValue += e.size(); 
            }
        }
        
        return returnValue;
    }

}
