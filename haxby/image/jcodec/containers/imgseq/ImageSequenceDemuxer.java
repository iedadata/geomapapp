package haxby.image.jcodec.containers.imgseq;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import haxby.image.jcodec.common.Codec;
import haxby.image.jcodec.common.Demuxer;
import haxby.image.jcodec.common.DemuxerTrack;
import haxby.image.jcodec.common.DemuxerTrackMeta;
import haxby.image.jcodec.common.TrackType;
import haxby.image.jcodec.common.io.NIOUtils;
import haxby.image.jcodec.common.logging.Logger;
import haxby.image.jcodec.common.model.Packet;
import haxby.image.jcodec.common.model.Packet.FrameType;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A demuxer that reads image files out of a folder.
 * 
 * Supports both sequences starting with 0 and 1 index.
 * 
 * @author Stanislav Vitvitskyy
 * 
 */
public class ImageSequenceDemuxer implements Demuxer, DemuxerTrack {

    private static final int VIDEO_FPS = 25;
    private String namePattern;
    private int frameNo;
    private Packet curFrame;
    private Codec codec;
    private int maxAvailableFrame;
    private int maxFrames;
    private String prevName;

    public ImageSequenceDemuxer(String namePattern, int maxFrames) throws IOException {
        this.namePattern = namePattern;
        this.maxFrames = maxFrames;
        this.maxAvailableFrame = -1;
        this.curFrame = loadFrame();
        // codec = JCodecUtil.detectDecoder(curFrame.getData());
        String lowerCase = namePattern.toLowerCase();
        if (lowerCase.endsWith(".png")) {
            codec = Codec.PNG;
        } else if (lowerCase.endsWith(".jpg") || lowerCase.endsWith(".jpeg")) {
            codec = Codec.JPEG;
        }
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public List<? extends DemuxerTrack> getTracks() {
        ArrayList<DemuxerTrack> tracks = new ArrayList<DemuxerTrack>();
        tracks.add(this);
        return tracks;
    }

    @Override
    public List<? extends DemuxerTrack> getVideoTracks() {
        return getTracks();
    }

    @Override
    public List<? extends DemuxerTrack> getAudioTracks() {
        return new ArrayList<DemuxerTrack>();
    }

    @Override
    public Packet nextFrame() throws IOException {
        try {
            return curFrame;
        } finally {
            curFrame = loadFrame();
        }
    }

    private Packet loadFrame() throws IOException {
        if (frameNo > maxFrames) {
            return null;
        }

        File file = null;
        do {
            String name = String.format(namePattern, frameNo);
            // In case the name doesn't contain placeholders, to prevent infinitely
            // looping around the same file.
            if (name.equals(prevName)) {
                return null;
            }
            prevName = name;
            file = new File(name);
            if (file.exists() || frameNo > 0)
                break;
            frameNo++;
        } while (frameNo < 2);

        if (file == null || !file.exists())
            return null;

        Packet ret = new Packet(NIOUtils.fetchFromFile(file), frameNo, VIDEO_FPS, 1, frameNo, FrameType.KEY, null, frameNo);
        ++frameNo;
        return ret;
    }

    private static final int MAX_MAX = 60 * 60 * 60 * 24; // Longest possible
                                                          // movie

    /**
     * Finds maximum frame of a sequence by bisecting the range.
     * 
     * Performs at max at max 48 Stat calls ( 2*log2(MAX_MAX) ).
     * 
     * @return
     */
    public int getMaxAvailableFrame() {
        if (maxAvailableFrame == -1) {

            int firstPoint = 0;
            for (int i = MAX_MAX; i > 0; i /= 2) {
                if (new File(String.format(namePattern, i)).exists()) {
                    firstPoint = i;
                    break;
                }
            }
            int pos = firstPoint;
            for (int interv = firstPoint / 2; interv > 1; interv /= 2) {
                if (new File(String.format(namePattern, pos + interv)).exists()) {
                    pos += interv;
                }
            }
            maxAvailableFrame = pos;
            Logger.info("Max frame found: " + maxAvailableFrame);
        }
        return Math.min(maxAvailableFrame, maxFrames);
    }

    @Override
    public DemuxerTrackMeta getMeta() {
        int durationFrames = getMaxAvailableFrame();
        return new DemuxerTrackMeta(TrackType.VIDEO, codec, (durationFrames + 1) * VIDEO_FPS, null, durationFrames + 1,
                null, null, null);
    }
}