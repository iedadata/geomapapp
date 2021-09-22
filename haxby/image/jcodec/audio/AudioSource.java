package haxby.image.jcodec.audio;
import java.io.IOException;
import java.nio.FloatBuffer;

import haxby.image.jcodec.common.AudioFormat;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public interface AudioSource {
    AudioFormat getFormat();
    
    int readFloat(FloatBuffer buffer) throws IOException;
}
