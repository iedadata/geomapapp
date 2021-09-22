package haxby.image.jcodec.containers.mxf.model;
import java.util.Iterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import haxby.image.jcodec.platform.Platform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * MXF demuxer
 * 
 * @author The JCodec project
 * 
 */
public class MXFUtil {
    public static <T> T resolveRef(List<MXFMetadata> metadata, UL refs, Class<T> class1) {
        List<T> res = resolveRefs(metadata, new UL[] { refs }, class1);
        return res.size() > 0 ? res.get(0) : null;
    }

    public static <T> List<T> resolveRefs(List<MXFMetadata> metadata, UL[] refs, Class<T> class1) {
        List<MXFMetadata> copy = new ArrayList<MXFMetadata>(metadata);
        for (Iterator<MXFMetadata> iterator = copy.iterator(); iterator.hasNext();) {
            MXFMetadata next = iterator.next();
            if (next.getUid() == null || !Platform.isAssignableFrom(class1, next.getClass()))
                iterator.remove();
        }

        List result = new ArrayList();
        for (int i = 0; i < refs.length; i++) {
            for (MXFMetadata meta : copy) {
                if (meta.getUid().equals(refs[i])) {
                    result.add(meta);
                }
            }
        }
        return result;
    }

    public static <T> T findMeta(Collection<MXFMetadata> metadata, Class<T> class1) {
        for (MXFMetadata mxfMetadata : metadata) {
            if (Platform.isAssignableFrom(mxfMetadata.getClass(), class1))
                return (T) mxfMetadata;
        }
        return null;
    }

    public static <T> List<T> findAllMeta(Collection<MXFMetadata> metadata, Class<T> class1) {
        List result = new ArrayList();
        for (MXFMetadata mxfMetadata : metadata) {
            if (Platform.isAssignableFrom(class1, mxfMetadata.getClass()))
                result.add((T) mxfMetadata);
        }
        return result;
    }

}
