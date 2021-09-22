package haxby.image.jcodec.containers.mp4.boxes;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A box storing a list of synch samples
 * 
 * @author The JCodec project
 * 
 */
public class PartialSyncSamplesBox extends SyncSamplesBox {
    public static final String STPS = "stps";

    public PartialSyncSamplesBox(Header header) {
        super(header);
    }
}
