package haxby.image.jcodec.containers.mp4.boxes;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MediaBox extends NodeBox {

    public static String fourcc() {
        return "mdia";
    }

    public static MediaBox createMediaBox() {
        return new MediaBox(new Header(fourcc()));
    }

    public MediaBox(Header atom) {
        super(atom);
    }

    public MediaInfoBox getMinf() {
        return NodeBox.findFirst(this, MediaInfoBox.class, "minf");
    }
}
