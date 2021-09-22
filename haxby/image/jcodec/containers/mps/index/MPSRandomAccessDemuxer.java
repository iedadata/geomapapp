package haxby.image.jcodec.containers.mps.index;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import haxby.image.jcodec.api.NotSupportedException;
import haxby.image.jcodec.common.DemuxerTrackMeta;
import haxby.image.jcodec.common.SeekableDemuxerTrack;
import haxby.image.jcodec.common.io.NIOUtils;
import haxby.image.jcodec.common.io.SeekableByteChannel;
import haxby.image.jcodec.common.model.Packet;
import haxby.image.jcodec.common.model.Packet.FrameType;
import haxby.image.jcodec.containers.mps.MPSUtils;
import haxby.image.jcodec.containers.mps.index.MPSIndex.MPSStreamIndex;
import haxby.image.jcodec.platform.Platform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Demuxer for MPEG Program Stream format with random access
 * 
 * Uses index to assist random access, see MPSIndexer
 * 
 * @author The JCodec project
 * 
 */
public class MPSRandomAccessDemuxer {

    private Stream[] streams;
    private long[] pesTokens;
    private int[] pesStreamIds;

    public MPSRandomAccessDemuxer(SeekableByteChannel ch, MPSIndex mpsIndex) throws IOException {
        pesTokens = mpsIndex.getPesTokens();
        pesStreamIds = mpsIndex.getPesStreamIds().flattern();
        MPSStreamIndex[] streamIndices = mpsIndex.getStreams();
        streams = new Stream[streamIndices.length];
        for (int i = 0; i < streamIndices.length; i++) {
            streams[i] = newStream(ch, streamIndices[i]);
        }
    }

    protected Stream newStream(SeekableByteChannel ch, MPSStreamIndex streamIndex) throws IOException {
        return new Stream(this, streamIndex, ch);
    }

    public Stream[] getStreams() {
        return streams;
    }

    public static class Stream extends MPSStreamIndex implements SeekableDemuxerTrack {

        private static final int MPEG_TIMESCALE = 90000;
        private int curPesIdx;
        private int curFrame;
        private ByteBuffer pesBuf;
        private int _seekToFrame = -1;
        protected SeekableByteChannel source;
        private long[] foffs;
		private MPSRandomAccessDemuxer demuxer;

        public Stream(MPSRandomAccessDemuxer demuxer, MPSStreamIndex streamIndex, SeekableByteChannel source) throws IOException {
            super(streamIndex.streamId, streamIndex.fsizes, streamIndex.fpts, streamIndex.fdur, streamIndex.sync);
			this.demuxer = demuxer;
            this.source = source;

            foffs = new long[fsizes.length];
            long curOff = 0;
            for (int i = 0; i < fsizes.length; i++) {
                foffs[i] = curOff;
                curOff += fsizes[i];
            }

            int[] seg = Platform.copyOfInt(streamIndex.getFpts(), 100);
            Arrays.sort(seg);

            _seekToFrame = 0;
            seekToFrame();
        }

        @Override
        public Packet nextFrame() throws IOException {
            seekToFrame();
            
            if(curFrame >= fsizes.length)
                return null;
            
            int fs = fsizes[curFrame];
            ByteBuffer result = ByteBuffer.allocate(fs);
            return _nextFrame(result);
        }

        private Packet _nextFrame(ByteBuffer buf) throws IOException {
            seekToFrame();

            if (curFrame >= fsizes.length)
                return null;

            int fs = fsizes[curFrame];

            ByteBuffer result = buf.duplicate();
            result.limit(result.position() + fs);

            while (result.hasRemaining()) {
                if (pesBuf.hasRemaining()) {
                    result.put(NIOUtils.read(pesBuf, Math.min(pesBuf.remaining(), result.remaining())));
                } else {
                    ++curPesIdx;
                    long posShift = 0;
                    while (demuxer.pesStreamIds[curPesIdx] != streamId) {
                        posShift += MPSIndex.pesLen(demuxer.pesTokens[curPesIdx]) + MPSIndex.leadingSize(demuxer.pesTokens[curPesIdx]);
                        ++curPesIdx;
                    }
                    skip(posShift + MPSIndex.leadingSize(demuxer.pesTokens[curPesIdx]));
                    int pesLen = MPSIndex.pesLen(demuxer.pesTokens[curPesIdx]);
                    pesBuf = fetch(pesLen);
                    MPSUtils.readPESHeader(pesBuf, 0);
                }
            }
            result.flip();

            Packet pkt = Packet.createPacket(result, fpts[curFrame], MPEG_TIMESCALE, fdur[curFrame], curFrame, sync.length == 0
                    || Arrays.binarySearch(sync, curFrame) >= 0 ? FrameType.KEY : FrameType.INTER, null);

            curFrame++;

            return pkt;
        }

        protected ByteBuffer fetch(int pesLen) throws IOException {
            return NIOUtils.fetchFromChannel(source, pesLen);
        }
        
        protected void skip(long leadingSize) throws IOException {
            source.setPosition(source.position() + leadingSize);
        }
        
        protected void reset() throws IOException {
            source.setPosition(0);
        }

        @Override
        public DemuxerTrackMeta getMeta() {
            return null;
        }

        @Override
        public boolean gotoFrame(long frameNo) {
            _seekToFrame = (int) frameNo;

            return true;
        }

        @Override
        public boolean gotoSyncFrame(long frameNo) {
            for (int i = 0; i < sync.length; i++) {
                if (sync[i] > frameNo) {
                    _seekToFrame = sync[i - 1];
                    return true;
                }
            }
            _seekToFrame = sync[sync.length - 1];
            return true;
        }

        private void seekToFrame() throws IOException {
            if (_seekToFrame == -1)
                return;
            curFrame = _seekToFrame;

            long payloadOff = foffs[curFrame];
            long posShift = 0;
            
            reset();

            for (curPesIdx = 0;; curPesIdx++) {
                if (demuxer.pesStreamIds[curPesIdx] == streamId) {
                    int payloadSize = MPSIndex.payLoadSize(demuxer.pesTokens[curPesIdx]);
                    if (payloadOff < payloadSize)
                        break;
                    payloadOff -= payloadSize;
                }
                posShift += MPSIndex.pesLen(demuxer.pesTokens[curPesIdx]) + MPSIndex.leadingSize(demuxer.pesTokens[curPesIdx]);
            }

            skip(posShift + MPSIndex.leadingSize(demuxer.pesTokens[curPesIdx]));
            pesBuf = fetch(MPSIndex.pesLen(demuxer.pesTokens[curPesIdx]));
            MPSUtils.readPESHeader(pesBuf, 0);
            NIOUtils.skip(pesBuf, (int) payloadOff);

            _seekToFrame = -1;
        }

        @Override
        public long getCurFrame() {
            return curFrame;
        }

        @Override
        public void seek(double second) {
            throw new NotSupportedException("");
        }
    }
}
