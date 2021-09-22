package haxby.image.jcodec.containers.mp4.demuxer;

import static haxby.image.jcodec.common.VideoCodecMeta.createSimpleVideoCodecMeta;

import java.nio.ByteBuffer;
import java.util.List;

import haxby.image.jcodec.codecs.aac.AACUtils;
import haxby.image.jcodec.codecs.h264.H264Utils;
import haxby.image.jcodec.codecs.h264.io.model.SeqParameterSet;
import haxby.image.jcodec.codecs.h264.mp4.AvcCBox;
import haxby.image.jcodec.common.AudioCodecMeta;
import haxby.image.jcodec.common.Codec;
import haxby.image.jcodec.common.DemuxerTrackMeta;
import haxby.image.jcodec.common.Ints;
import haxby.image.jcodec.common.TrackType;
import haxby.image.jcodec.common.VideoCodecMeta;
import haxby.image.jcodec.common.model.ColorSpace;
import haxby.image.jcodec.common.model.RationalLarge;
import haxby.image.jcodec.containers.mp4.BoxUtil;
import haxby.image.jcodec.containers.mp4.MP4TrackType;
import haxby.image.jcodec.containers.mp4.boxes.AudioSampleEntry;
import haxby.image.jcodec.containers.mp4.boxes.Box;
import haxby.image.jcodec.containers.mp4.boxes.NodeBox;
import haxby.image.jcodec.containers.mp4.boxes.PixelAspectExt;
import haxby.image.jcodec.containers.mp4.boxes.SampleEntry;
import haxby.image.jcodec.containers.mp4.boxes.SyncSamplesBox;
import haxby.image.jcodec.containers.mp4.boxes.TrackHeaderBox;
import haxby.image.jcodec.containers.mp4.boxes.TrakBox;
import haxby.image.jcodec.containers.mp4.boxes.VideoSampleEntry;
import haxby.image.jcodec.platform.Platform;

public class MP4DemuxerTrackMeta extends DemuxerTrackMeta {
    private SampleEntry[] sampleEntries;
    private ByteBuffer codecPrivateOpaque;

    public MP4DemuxerTrackMeta(TrackType type, Codec codec, double totalDuration, int[] seekFrames, int totalFrames,
            ByteBuffer codecPrivate, VideoCodecMeta videoCodecMeta, AudioCodecMeta audioCodecMeta,
            SampleEntry[] sampleEntries, ByteBuffer codecMetaOpaque) {
        super(type, codec, totalDuration, seekFrames, totalFrames, codecPrivate, videoCodecMeta, audioCodecMeta);
        this.sampleEntries = sampleEntries;
        this.codecPrivateOpaque = codecMetaOpaque;
    }

    public SampleEntry[] getSampleEntries() {
        return sampleEntries;
    }

    public static DemuxerTrackMeta fromTrack(AbstractMP4DemuxerTrack track) {
        TrakBox trak = track.getBox();
        SyncSamplesBox stss = NodeBox.findFirstPath(trak, SyncSamplesBox.class, Box.path("mdia.minf.stbl.stss"));
        int[] syncSamples = stss == null ? null : stss.getSyncSamples();

        int[] seekFrames;
        if (syncSamples == null) {
            // all frames are I-frames
            seekFrames = new int[(int) track.getFrameCount()];
            for (int i = 0; i < seekFrames.length; i++) {
                seekFrames[i] = i;
            }
        } else {
            seekFrames = Platform.copyOfInt(syncSamples, syncSamples.length);
            for (int i = 0; i < seekFrames.length; i++)
                seekFrames[i]--;
        }

        MP4TrackType type = track.getType();
        TrackType t = type == null ? TrackType.OTHER : type.getTrackType();
        VideoCodecMeta videoCodecMeta = getVideoCodecMeta(track, trak, type);
        AudioCodecMeta audioCodecMeta = getAudioCodecMeta(track, type);
        RationalLarge duration = track.getDuration();
        double sec = (double) duration.getNum() / duration.getDen();
        int frameCount = Ints.checkedCast(track.getFrameCount());
        ByteBuffer opaque = getCodecPrivateOpaque(Codec.codecByFourcc(track.getFourcc()),
                track.getSampleEntries()[0]);
        MP4DemuxerTrackMeta meta = new MP4DemuxerTrackMeta(t, Codec.codecByFourcc(track.getFourcc()), sec, seekFrames,
                frameCount, getCodecPrivate(track), videoCodecMeta, audioCodecMeta, track.getSampleEntries(), opaque);

        meta.setIndex(track.getBox().getTrackHeader().getNo());
        if (type == MP4TrackType.VIDEO) {
            TrackHeaderBox tkhd = NodeBox.findFirstPath(trak, TrackHeaderBox.class, Box.path("tkhd"));

            DemuxerTrackMeta.Orientation orientation;
            if (tkhd.isOrientation90())
                orientation = DemuxerTrackMeta.Orientation.D_90;
            else if (tkhd.isOrientation180())
                orientation = DemuxerTrackMeta.Orientation.D_180;
            else if (tkhd.isOrientation270())
                orientation = DemuxerTrackMeta.Orientation.D_270;
            else
                orientation = DemuxerTrackMeta.Orientation.D_0;

            meta.setOrientation(orientation);
        }

        return meta;
    }

    public static ByteBuffer getCodecPrivateOpaque(Codec codec, SampleEntry se) {
        if (codec == Codec.H264) {
            Box b = NodeBox.findFirst(se, Box.class, "avcC");
            return b != null ? BoxUtil.writeBox(b) : null;
        } else if (codec == Codec.AAC) {
            Box b = NodeBox.findFirst(se, Box.class, "esds");
            if (b == null) {
                b = NodeBox.findFirstPath(se, Box.class, new String[] { null, "esds" });
            }
            return b != null ? BoxUtil.writeBox(b) : null;
        }
        return null;
    }

    private static AudioCodecMeta getAudioCodecMeta(AbstractMP4DemuxerTrack track, MP4TrackType type) {
        AudioCodecMeta audioCodecMeta = null;
        if (type == MP4TrackType.SOUND) {
            AudioSampleEntry ase = (AudioSampleEntry) track.getSampleEntries()[0];
            audioCodecMeta = AudioCodecMeta.fromAudioFormat(ase.getFormat());
        }
        return audioCodecMeta;
    }

    private static VideoCodecMeta getVideoCodecMeta(AbstractMP4DemuxerTrack track, TrakBox trak, MP4TrackType type) {
        VideoCodecMeta videoCodecMeta = null;
        if (type == MP4TrackType.VIDEO) {
            videoCodecMeta = createSimpleVideoCodecMeta(trak.getCodedSize(), getColorInfo(track));
            PixelAspectExt pasp = NodeBox.findFirst(track.getSampleEntries()[0], PixelAspectExt.class, "pasp");
            if (pasp != null)
                videoCodecMeta.setPixelAspectRatio(pasp.getRational());
        }
        return videoCodecMeta;
    }

    protected static ColorSpace getColorInfo(AbstractMP4DemuxerTrack track) {
        Codec codec = Codec.codecByFourcc(track.getFourcc());
        if (codec == Codec.H264) {
            AvcCBox avcC = H264Utils.parseAVCC((VideoSampleEntry) track.getSampleEntries()[0]);
            if (avcC != null) {
                List<ByteBuffer> spsList = avcC.getSpsList();
                if (spsList.size() > 0) {
                    SeqParameterSet sps = SeqParameterSet.read(spsList.get(0).duplicate());
                    return sps.getChromaFormatIdc();
                }
            }
        }
        return null;
    }

    public static ByteBuffer getCodecPrivate(AbstractMP4DemuxerTrack track) {
        Codec codec = Codec.codecByFourcc(track.getFourcc());
        if (codec == Codec.H264) {
            AvcCBox avcC = H264Utils.parseAVCC((VideoSampleEntry) track.getSampleEntries()[0]);
            return avcC != null ? H264Utils.avcCToAnnexB(avcC) : null;
        } else if (codec == Codec.AAC) {
            return AACUtils.getCodecPrivate(track.getSampleEntries()[0]);
        }
        // This codec does not have private section
        return null;
    }

    public ByteBuffer getCodecPrivateOpaque() {
        return codecPrivateOpaque;
    }
}
