package haxby.image.jcodec.containers.mxf.model;
import java.util.Iterator;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Map.Entry;

import haxby.image.jcodec.common.logging.Logger;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class GenericDescriptor extends MXFInterchangeObject {
    private UL[] locators;
    private UL[] subDescriptors;

    public GenericDescriptor(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        for (Iterator<Entry<Integer, ByteBuffer>> it = tags.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, ByteBuffer> entry = it.next();
            
            ByteBuffer _bb = entry.getValue();

            switch (entry.getKey()) {
            case 0x2f01:
                locators = readULBatch(_bb);
                break;
            case 0x3F01:
                subDescriptors = readULBatch(_bb);
                break;
            default:
                Logger.warn(String.format("Unknown tag [ " + ul + "]: %04x", entry.getKey()));
                continue;
            }
            it.remove();
        }
    }

    public UL[] getLocators() {
        return locators;
    }

    public UL[] getSubDescriptors() {
        return subDescriptors;
    }
}