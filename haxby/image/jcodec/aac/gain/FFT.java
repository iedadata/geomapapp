package haxby.image.jcodec.aac.gain;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 *
 * @author in-somnia
 */
//complex FFT of length 128/16, inplace
class FFT {

    private static final float[][] FFT_TABLE_128 = { { 1.0f, -0.0f }, { 0.99879545f, -0.049067676f },
            { 0.9951847f, -0.09801714f }, { 0.9891765f, -0.14673047f }, { 0.98078525f, -0.19509032f },
            { 0.97003126f, -0.24298018f }, { 0.95694035f, -0.29028466f }, { 0.94154406f, -0.33688986f },
            { 0.9238795f, -0.38268343f }, { 0.9039893f, -0.42755508f }, { 0.8819213f, -0.47139674f },
            { 0.8577286f, -0.51410276f }, { 0.8314696f, -0.55557024f }, { 0.8032075f, -0.5956993f },
            { 0.77301043f, -0.6343933f }, { 0.7409511f, -0.671559f }, { 0.70710677f, -0.70710677f },
            { 0.671559f, -0.7409511f }, { 0.6343933f, -0.77301043f }, { 0.5956993f, -0.8032075f },
            { 0.55557024f, -0.8314696f }, { 0.51410276f, -0.8577286f }, { 0.47139674f, -0.8819213f },
            { 0.42755508f, -0.9039893f }, { 0.38268343f, -0.9238795f }, { 0.33688986f, -0.94154406f },
            { 0.29028466f, -0.95694035f }, { 0.24298018f, -0.97003126f }, { 0.19509032f, -0.98078525f },
            { 0.14673047f, -0.9891765f }, { 0.09801714f, -0.9951847f }, { 0.049067676f, -0.99879545f },
            { 6.123234E-17f, -1.0f }, { -0.049067676f, -0.99879545f }, { -0.09801714f, -0.9951847f },
            { -0.14673047f, -0.9891765f }, { -0.19509032f, -0.98078525f }, { -0.24298018f, -0.97003126f },
            { -0.29028466f, -0.95694035f }, { -0.33688986f, -0.94154406f }, { -0.38268343f, -0.9238795f },
            { -0.42755508f, -0.9039893f }, { -0.47139674f, -0.8819213f }, { -0.51410276f, -0.8577286f },
            { -0.55557024f, -0.8314696f }, { -0.5956993f, -0.8032075f }, { -0.6343933f, -0.77301043f },
            { -0.671559f, -0.7409511f }, { -0.70710677f, -0.70710677f }, { -0.7409511f, -0.671559f },
            { -0.77301043f, -0.6343933f }, { -0.8032075f, -0.5956993f }, { -0.8314696f, -0.55557024f },
            { -0.8577286f, -0.51410276f }, { -0.8819213f, -0.47139674f }, { -0.9039893f, -0.42755508f },
            { -0.9238795f, -0.38268343f }, { -0.94154406f, -0.33688986f }, { -0.95694035f, -0.29028466f },
            { -0.97003126f, -0.24298018f }, { -0.98078525f, -0.19509032f }, { -0.9891765f, -0.14673047f },
            { -0.9951847f, -0.09801714f }, { -0.99879545f, -0.049067676f } };
    private static final float[][] FFT_TABLE_16 = { { 1.0f, -0.0f }, { 0.9238795f, -0.38268343f },
            { 0.70710677f, -0.70710677f }, { 0.38268343f, -0.9238795f }, { 6.123234E-17f, -1.0f },
            { -0.38268343f, -0.9238795f }, { -0.70710677f, -0.70710677f }, { -0.9238795f, -0.38268343f } };

    static void process(float[][] _in, int n) {
        final int ln = (int) Math.round(Math.log(n) / Math.log(2));
        final float[][] table = (n == 128) ? FFT_TABLE_128 : FFT_TABLE_16;

        // bit-reversal
        final float[][] rev = new float[n][2];
        int i, ii = 0;
        for (i = 0; i < n; i++) {
            rev[i][0] = _in[ii][0];
            rev[i][1] = _in[ii][1];
            int k = n >> 1;
            while (ii >= k && k > 0) {
                ii -= k;
                k >>= 1;
            }
            ii += k;
        }
        for (i = 0; i < n; i++) {
            _in[i][0] = rev[i][0];
            _in[i][1] = rev[i][1];
        }

        // calculation
        int blocks = n / 2;
        int size = 2;
        int j, k, l, k0, k1, size2;
        float[] a = new float[2];
        for (i = 0; i < ln; i++) {
            size2 = size / 2;
            k0 = 0;
            k1 = size2;
            for (j = 0; j < blocks; ++j) {
                l = 0;
                for (k = 0; k < size2; ++k) {
                    a[0] = _in[k1][0] * table[l][0] - _in[k1][1] * table[l][1];
                    a[1] = _in[k1][0] * table[l][1] + _in[k1][1] * table[l][0];
                    _in[k1][0] = _in[k0][0] - a[0];
                    _in[k1][1] = _in[k0][1] - a[1];
                    _in[k0][0] += a[0];
                    _in[k0][1] += a[1];
                    l += blocks;
                    k0++;
                    k1++;
                }
                k0 += size2;
                k1 += size2;
            }
            blocks = blocks / 2;
            size = size * 2;
        }
    }
}
