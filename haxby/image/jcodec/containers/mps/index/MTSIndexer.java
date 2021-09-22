package haxby.image.jcodec.containers.mps.index;
import static haxby.image.jcodec.common.Preconditions.checkState;
import static haxby.image.jcodec.containers.mps.MPSUtils.mediaStream;
import static haxby.image.jcodec.containers.mps.MPSUtils.readPESHeader;
import static haxby.image.jcodec.containers.mps.index.MTSIndex.createMTSProgram;

import java.io.File;
import java.io.IOException;
import java.lang.System;
import java.nio.ByteBuffer;

import haxby.image.jcodec.common.io.NIOUtils;
import haxby.image.jcodec.common.io.SeekableByteChannel;
import haxby.image.jcodec.common.io.NIOUtils.FileReader;
import haxby.image.jcodec.common.logging.Logger;
import haxby.image.jcodec.containers.mps.MTSUtils;
import haxby.image.jcodec.containers.mps.PESPacket;
import haxby.image.jcodec.containers.mps.index.MTSIndex.MTSProgram;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Indexes MPEG TS file for the purpose of quick random access in the future
 * 
 * @author The JCodec project
 * 
 */
public class MTSIndexer {
    public static final int BUFFER_SIZE = 188 << 9;
    private MTSAnalyser[] indexers;

    public void index(File source, NIOUtils.FileReaderListener listener) throws IOException {
        indexReader(listener, MTSUtils.getMediaPids(source)).readFile(source, BUFFER_SIZE, listener);
    }

    public void indexChannel(SeekableByteChannel source, NIOUtils.FileReaderListener listener) throws IOException {
        indexReader(listener, MTSUtils.getMediaPidsFromChannel(source)).readChannel(source, BUFFER_SIZE, listener);
    }

    public FileReader indexReader(NIOUtils.FileReaderListener listener, int[] targetGuids) throws IOException {
        indexers = new MTSAnalyser[targetGuids.length];
        for (int i = 0; i < targetGuids.length; i++) {
            indexers[i] = new MTSAnalyser(targetGuids[i]);
        }
        return new MTSFileReader(this);
    }

    public MTSIndex serialize() {
        MTSProgram[] programs = new MTSProgram[indexers.length];
        for (int i = 0; i < indexers.length; i++)
            programs[i] = indexers[i].serializeTo();
        return new MTSIndex(programs);
    }

    private static final class MTSFileReader extends NIOUtils.FileReader {
        private MTSIndexer indexer;

        public MTSFileReader(MTSIndexer indexer) {
            this.indexer = indexer;
        }

        protected void data(ByteBuffer data, long filePos) {
            analyseBuffer(data, filePos);
        }

        protected void analyseBuffer(ByteBuffer buf, long pos) {
            while (buf.hasRemaining()) {
                ByteBuffer tsBuf = NIOUtils.read(buf, 188);
                pos += 188;
                checkState(0x47 == (tsBuf.get() & 0xff));
                int guidFlags = ((tsBuf.get() & 0xff) << 8) | (tsBuf.get() & 0xff);
                int guid = (int) guidFlags & 0x1fff;

                for (int i = 0; i < indexer.indexers.length; i++) {

                    if (guid == indexer.indexers[i].targetGuid) {
                        int payloadStart = (guidFlags >> 14) & 0x1;
                        int b0 = tsBuf.get() & 0xff;
                        int counter = b0 & 0xf;
                        if ((b0 & 0x20) != 0) {
                            NIOUtils.skip(tsBuf, tsBuf.get() & 0xff);
                        }
                        indexer.indexers[i].analyseBuffer(tsBuf, pos - tsBuf.remaining());
                    }
                }
            }
        }

        @Override
        protected void done() {
            for (MTSAnalyser mtsAnalyser : indexer.indexers) {
                mtsAnalyser.finishAnalyse();
            }
        }
    }

    private static class MTSAnalyser extends BaseIndexer {

        private int targetGuid;
        private long predFileStartInTsPkt;

        public MTSAnalyser(int targetGuid) {
            this.targetGuid = targetGuid;
        }

        public MTSProgram serializeTo() {
            return createMTSProgram(super.serialize(), targetGuid);
        }

        protected void pes(ByteBuffer pesBuffer, long start, int pesLen, int stream) {
            if (!mediaStream(stream))
                return;
            Logger.debug(String.format("PES: %08x, %d", start, pesLen));
            PESPacket pesHeader = readPESHeader(pesBuffer, start);
            int leadingTsPkt = 0;// pesBuffer.position();
            if (predFileStartInTsPkt != start) {
                leadingTsPkt = (int) (start / 188 - predFileStartInTsPkt);
            }
            predFileStartInTsPkt = (start + pesLen) / 188;
            int tsPktInPes = (int) (predFileStartInTsPkt - start / 188);
            savePESMeta(stream, MPSIndex.makePESToken(leadingTsPkt, tsPktInPes, pesBuffer.remaining()));
            getAnalyser(stream).pkt(pesBuffer, pesHeader);
        }
    }

    public static void main1(String[] args) throws IOException {
        File src = new File(args[0]);

        MTSIndexer indexer = new MTSIndexer();
        indexer.index(src, new NIOUtils.FileReaderListener() {
            public void progress(int percentDone) {
                System.out.println(percentDone);
            }
        });
        MTSIndex index = indexer.serialize();
        NIOUtils.writeTo(index.serialize(), new File(args[1]));
    }
}