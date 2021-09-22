package haxby.image.jcodec.codecs.mpeg4.es;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import haxby.image.jcodec.common.JCodecUtil2;
import haxby.image.jcodec.common.io.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public abstract class Descriptor {
    private int _tag;
    private int size;

    public Descriptor(int tag, int size) {
        this._tag = tag;
        this.size = size;
    }

    public void write(ByteBuffer out) {
        ByteBuffer fork = out.duplicate();
        NIOUtils.skip(out, 5);
        doWrite(out);

        int length = out.position() - fork.position() - 5;
        fork.put((byte) _tag);
        JCodecUtil2.writeBER32(fork, length);
    }

    protected abstract void doWrite(ByteBuffer out);

    int getTag() {
        return _tag;
    }
}
