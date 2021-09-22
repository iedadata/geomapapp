package haxby.image.jcodec.movtool;


import java.util.List;

import haxby.image.jcodec.containers.mp4.boxes.MovieBox;
import haxby.image.jcodec.containers.mp4.boxes.MovieFragmentBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Like MP4Edit
 * 
 * @author The JCodec project
 * 
 */
public class CompoundMP4Edit implements MP4Edit {

    private List<MP4Edit> edits;

    public CompoundMP4Edit(List<MP4Edit> edits) {
        this.edits = edits;
    }

    @Override
    public void applyToFragment(MovieBox mov, MovieFragmentBox[] fragmentBox) {
        for (MP4Edit command : edits) {
            command.applyToFragment(mov, fragmentBox);
        }
    }

    @Override
    public void apply(MovieBox mov) {
        for (MP4Edit command : edits) {
            command.apply(mov);
        }
    }
}