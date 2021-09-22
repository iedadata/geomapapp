package haxby.image.jcodec.api.transcode;

import haxby.image.jcodec.common.model.ColorSpace;
import haxby.image.jcodec.common.model.Picture;

public interface PixelStore {
    public static class LoanerPicture {
        private Picture picture;
        private int refCnt;

        public LoanerPicture(Picture picture, int refCnt) {
            this.picture = picture;
            this.refCnt = refCnt;
        }

        public Picture getPicture() {
            return picture;
        }

        public int getRefCnt() {
            return refCnt;
        }

        public void decRefCnt() {
            -- refCnt;
        }

        public boolean unused() {
            return refCnt <= 0;
        }

        public void incRefCnt() {
            ++refCnt;
        }
    }

    LoanerPicture getPicture(int width, int height, ColorSpace color);

    void putBack(LoanerPicture frame);

    void retake(LoanerPicture frame);
}