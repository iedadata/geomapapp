package haxby.image.jcodec.containers.mp4.demuxer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import haxby.image.jcodec.common.IntArrayList;
import haxby.image.jcodec.common.io.NIOUtils;
import haxby.image.jcodec.common.io.SeekableByteChannel;
import haxby.image.jcodec.common.model.TapeTimecode;
import haxby.image.jcodec.containers.mp4.MP4Packet;
import haxby.image.jcodec.containers.mp4.QTTimeUtil;
import haxby.image.jcodec.containers.mp4.boxes.Box;
import haxby.image.jcodec.containers.mp4.boxes.ChunkOffsets64Box;
import haxby.image.jcodec.containers.mp4.boxes.ChunkOffsetsBox;
import haxby.image.jcodec.containers.mp4.boxes.MovieBox;
import haxby.image.jcodec.containers.mp4.boxes.NodeBox;
import haxby.image.jcodec.containers.mp4.boxes.SampleToChunkBox;
import haxby.image.jcodec.containers.mp4.boxes.TimeToSampleBox;
import haxby.image.jcodec.containers.mp4.boxes.TimecodeSampleEntry;
import haxby.image.jcodec.containers.mp4.boxes.TrakBox;
import haxby.image.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry;
import haxby.image.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Timecode track, provides timecode information for video track
 * 
 * @author The JCodec project
 * 
 */
public class TimecodeMP4DemuxerTrack {

    private TrakBox box;
    private TimeToSampleEntry[] timeToSamples;
    private int[] sampleCache;
    private TimecodeSampleEntry tse;
    private SeekableByteChannel input;
    private MovieBox movie;
    private long[] chunkOffsets;
    private SampleToChunkEntry[] sampleToChunks;

    public TimecodeMP4DemuxerTrack(MovieBox movie, TrakBox trak, SeekableByteChannel input) throws IOException {
        this.box = trak;
        this.input = input;
        this.movie = movie;

        NodeBox stbl = trak.getMdia().getMinf().getStbl();

        TimeToSampleBox stts = NodeBox.findFirst(stbl, TimeToSampleBox.class, "stts");
        SampleToChunkBox stsc = NodeBox.findFirst(stbl, SampleToChunkBox.class, "stsc");
        ChunkOffsetsBox stco = NodeBox.findFirst(stbl, ChunkOffsetsBox.class, "stco");
        ChunkOffsets64Box co64 = NodeBox.findFirst(stbl, ChunkOffsets64Box.class, "co64");

        timeToSamples = stts.getEntries();
        chunkOffsets = stco != null ? stco.getChunkOffsets() : co64.getChunkOffsets();
        sampleToChunks = stsc.getSampleToChunk();
        if (chunkOffsets.length == 1) {
            cacheSamples(sampleToChunks, chunkOffsets);
        }

        tse = (TimecodeSampleEntry) box.getSampleEntries()[0];
    }

    public MP4Packet getTimecode(MP4Packet pkt) throws IOException {

        long tv = QTTimeUtil.editedToMedia(box, box.rescale(pkt.getPts(), pkt.getTimescale()), movie.getTimescale());
        int sample;
        int ttsInd = 0, ttsSubInd = 0;
        for (sample = 0; sample < sampleCache.length - 1; sample++) {
            int dur = timeToSamples[ttsInd].getSampleDuration();
            if (tv < dur)
                break;
            tv -= dur;
            ttsSubInd++;
            if (ttsInd < timeToSamples.length - 1 && ttsSubInd >= timeToSamples[ttsInd].getSampleCount())
                ttsInd++;
        }

        int frameNo = (int) ((((2 * tv * tse.getTimescale()) / box.getTimescale()) / tse.getFrameDuration()) + 1) / 2;

        return MP4Packet.createMP4PacketWithTimecode(pkt, _getTimecode(getTimecodeSample(sample), frameNo, tse));
    }

    private int getTimecodeSample(int sample) throws IOException {
        if (sampleCache != null)
            return sampleCache[sample];
        else {
            synchronized (input) {
                int stscInd, stscSubInd;
                for (stscInd = 0, stscSubInd = sample; stscInd < sampleToChunks.length
                        && stscSubInd >= sampleToChunks[stscInd].getCount(); stscSubInd -= sampleToChunks[stscInd]
                        .getCount(), stscInd++)
                    ;
                long offset = chunkOffsets[stscInd]
                        + (Math.min(stscSubInd, sampleToChunks[stscInd].getCount() - 1) << 2);
                if (input.position() != offset)
                    input.setPosition(offset);
                ByteBuffer buf = NIOUtils.fetchFromChannel(input, 4);
                return buf.getInt();
            }
        }
    }

    private static TapeTimecode _getTimecode(int startCounter, int frameNo, TimecodeSampleEntry entry) {
        return TapeTimecode.tapeTimecode(frameNo + startCounter, entry.isDropFrame(), entry.getNumFrames() & 0xff);
    }

    private void cacheSamples(SampleToChunkEntry[] sampleToChunks, long[] chunkOffsets) throws IOException {
        synchronized (input) {
            int stscInd = 0;
            IntArrayList ss = IntArrayList.createIntArrayList();
            for (int chunkNo = 0; chunkNo < chunkOffsets.length; chunkNo++) {
                int nSamples = sampleToChunks[stscInd].getCount();
                if (stscInd < sampleToChunks.length - 1 && chunkNo + 1 >= sampleToChunks[stscInd + 1].getFirst())
                    stscInd++;
                long offset = chunkOffsets[chunkNo];
                input.setPosition(offset);
                ByteBuffer buf = NIOUtils.fetchFromChannel(input, nSamples * 4);
                for (int i = 0; i < nSamples; i++) {
                    ss.add(buf.getInt());
                }
            }
            sampleCache = ss.toArray();
        }
    }

    /**
     * 
     * @return
     * @throws IOException 
     * @deprecated Use getTimecode to automatically populate tape timecode for
     *             each frame
     */
    @Deprecated
    public int getStartTimecode() throws IOException {
        return getTimecodeSample(0);
    }

    public TrakBox getBox() {
        return box;
    }

    public int parseTimecode(String tc) {
        String[] split = tc.split(":");
        TimecodeSampleEntry tmcd = NodeBox.findFirstPath(box, TimecodeSampleEntry.class, Box.path("mdia.minf.stbl.stsd.tmcd"));
        byte nf = tmcd.getNumFrames();

        return Integer.parseInt(split[3]) + Integer.parseInt(split[2]) * nf + Integer.parseInt(split[1]) * 60 * nf
                + Integer.parseInt(split[0]) * 3600 * nf;
    }

    public int timeCodeToFrameNo(String timeCode) throws Exception {
        if (isValidTimeCode(timeCode)) {
            int movieFrame = parseTimecode(timeCode.trim()) - sampleCache[0];
            int frameRate = tse.getNumFrames();
            long framesInTimescale = movieFrame * tse.getTimescale();
            long mediaToEdited = QTTimeUtil.mediaToEdited(box, framesInTimescale / frameRate, movie.getTimescale())
                    * frameRate;
            return (int) (mediaToEdited / box.getTimescale());
        }
        return -1;
    }

    private static boolean isValidTimeCode(String input) {
        Pattern p = Pattern.compile("[0-9][0-9]:[0-5][0-9]:[0-5][0-9]:[0-2][0-9]");
        Matcher m = p.matcher(input);
        if (input != null && !input.trim().equals("") && m.matches()) {
            return true;
        }
        return false;
    }
}
