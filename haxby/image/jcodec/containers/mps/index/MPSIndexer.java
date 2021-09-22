package haxby.image.jcodec.containers.mps.index;
import static haxby.image.jcodec.containers.mps.MPSUtils.mediaStream;
import static haxby.image.jcodec.containers.mps.MPSUtils.readPESHeader;

import java.io.File;
import java.io.IOException;
import java.lang.System;
import java.nio.ByteBuffer;

import haxby.image.jcodec.common.io.NIOUtils;
import haxby.image.jcodec.common.io.SeekableByteChannel;
import haxby.image.jcodec.common.io.NIOUtils.FileReader;
import haxby.image.jcodec.containers.mps.PESPacket;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Indexes MPEG PS file for the purpose of quick random access in the future
 * 
 * @author The JCodec project
 * 
 */
public class MPSIndexer extends BaseIndexer {
    private long predFileStart;

    public void index(File source, NIOUtils.FileReaderListener listener) throws IOException {
        newReader().readFile(source, 0x10000, listener);
    }

    public void indexChannel(SeekableByteChannel source, NIOUtils.FileReaderListener listener) throws IOException {
        newReader().readChannel(source, 0x10000, listener);
    }

    private FileReader newReader() {
        final MPSIndexer self = this;
        return new NIOUtils.FileReader() {
            @Override
            protected void data(ByteBuffer data, long filePos) {
                self.analyseBuffer(data, filePos);
            }
            @Override
            protected void done() {
                self.finishAnalyse();
            }
        };
    }

    @Override
    protected void pes(ByteBuffer pesBuffer, long start, int pesLen, int stream) {
        if (!mediaStream(stream))
            return;
        PESPacket pesHeader = readPESHeader(pesBuffer, start);
        int leading = 0;
        if (predFileStart != start) {
            leading += (int) (start - predFileStart);
        }
        predFileStart = start + pesLen;
        savePESMeta(stream, MPSIndex.makePESToken(leading, pesLen, pesBuffer.remaining()));
        getAnalyser(stream).pkt(pesBuffer, pesHeader);
    }

    public static void main1(String[] args) throws IOException {
        MPSIndexer indexer = new MPSIndexer();
        indexer.index(new File(args[0]), new NIOUtils.FileReaderListener() {
            public void progress(int percentDone) {
                System.out.println(percentDone);
            }
        });
        ByteBuffer index = ByteBuffer.allocate(0x10000);
        indexer.serialize().serializeTo(index);
        NIOUtils.writeTo(index, new File(args[1]));
    }
}