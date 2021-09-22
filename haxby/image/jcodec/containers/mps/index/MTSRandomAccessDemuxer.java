package haxby.image.jcodec.containers.mps.index;
import static haxby.image.jcodec.common.Preconditions.checkState;

import java.io.IOException;
import java.nio.ByteBuffer;

import haxby.image.jcodec.common.io.NIOUtils;
import haxby.image.jcodec.common.io.SeekableByteChannel;
import haxby.image.jcodec.containers.mps.index.MPSIndex.MPSStreamIndex;
import haxby.image.jcodec.containers.mps.index.MTSIndex.MTSProgram;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MTSRandomAccessDemuxer {

    private MTSProgram[] programs;
    private SeekableByteChannel ch;

    public MTSRandomAccessDemuxer(SeekableByteChannel ch, MTSIndex index) {
        programs = index.getPrograms();
        this.ch = ch;
    }

    public int[] getGuids() {
        int[] guids = new int[programs.length];
        for (int i = 0; i < programs.length; i++)
            guids[i] = programs[i].getTargetGuid();
        return guids;
    }

    public MPSRandomAccessDemuxer getProgramDemuxer(final int tgtGuid) throws IOException {
        MPSIndex index = getProgram(tgtGuid);
        return new MPSRandomAccessDemuxer(ch, index) {
            @Override
            protected Stream newStream(SeekableByteChannel ch, MPSStreamIndex streamIndex) throws IOException {
                return new Stream(this, streamIndex, ch) {
                    @Override
                    protected ByteBuffer fetch(int pesLen) throws IOException {
                        ByteBuffer bb = ByteBuffer.allocate(pesLen * 188);
                        
                        for(int i = 0; i < pesLen; i++) {
                            ByteBuffer tsBuf = NIOUtils.fetchFromChannel(source, 188);
                            checkState(0x47 == (tsBuf.get() & 0xff));
                            int guidFlags = ((tsBuf.get() & 0xff) << 8) | (tsBuf.get() & 0xff);
                            int guid = (int) guidFlags & 0x1fff;
                            if(guid != tgtGuid)
                                continue;
                            int payloadStart = (guidFlags >> 14) & 0x1;
                            int b0 = tsBuf.get() & 0xff;
                            int counter = b0 & 0xf;
                            if ((b0 & 0x20) != 0) {
                                NIOUtils.skip(tsBuf, tsBuf.get() & 0xff);
                            }
                            bb.put(tsBuf);
                        }
                        bb.flip();
                        return bb;
                    }

                    @Override
                    protected void skip(long leadingSize) throws IOException {
                        source.setPosition(source.position() + leadingSize * 188);
                    }

                    @Override
                    protected void reset() throws IOException {
                        source.setPosition(0);
                    }

                };
            }
        };
    }

    private MPSIndex getProgram(int guid) {
        for (MTSProgram mtsProgram : programs) {
            if (mtsProgram.getTargetGuid() == guid)
                return mtsProgram;
        }
        return null;
    }
}