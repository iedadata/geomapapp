package haxby.image.jcodec.common.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.lang.IllegalArgumentException;
import java.lang.NullPointerException;
import java.lang.StringBuilder;
import java.lang.System;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import haxby.image.jcodec.common.IntArrayList;
import haxby.image.jcodec.common.io.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Simple JSON serializer, introduced because jcodec can not use dependencies as
 * they bring frastration on some platforms
 * 
 * @author The JCodec project
 */
public class ToJSON {
    private final static Set<Class> primitive = new HashSet<Class>();
    private final static Set<String> omitMethods = new HashSet<String>();

    static {
        primitive.add(Boolean.class);
        primitive.add(Byte.class);
        primitive.add(Short.class);
        primitive.add(Integer.class);
        primitive.add(Long.class);
        primitive.add(Float.class);
        primitive.add(Double.class);
        primitive.add(Character.class);
    }

    static {
        omitMethods.add("getClass");
        omitMethods.add("get");
    }

    /**
     * Converts an object to JSON
     * 
     * @param obj
     * @return
     */
    public static String toJSON(Object obj) {
        StringBuilder builder = new StringBuilder();
        IntArrayList stack = IntArrayList.createIntArrayList();
        toJSONSub(obj, stack, builder);
        return builder.toString();
    }

    private static void toJSONSub(Object obj, IntArrayList stack, StringBuilder builder) {
        if(obj == null) {
            builder.append("null");
            return;
        }
        
        String className = obj.getClass().getName();
        if(className.startsWith("java.lang") && !className.equals("java.lang.String")) {
            builder.append("null");
            return;
        }
        
        int id = System.identityHashCode(obj);
        if (stack.contains(id)) {
            builder.append("null");
            return;
        }
        stack.push(id);

        if (obj instanceof ByteBuffer)
            obj = NIOUtils.toArray((ByteBuffer) obj);

        if (obj == null) {
            builder.append("null");
        } else if (obj instanceof String) {
            builder.append("\"");
            escape((String) obj, builder);
            builder.append("\"");
        } else if (obj instanceof Map) {
            Iterator it = ((Map) obj).entrySet().iterator();
            builder.append("{");
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                builder.append("\"");
                builder.append(e.getKey());
                builder.append("\":");
                toJSONSub(e.getValue(), stack, builder);
                if (it.hasNext())
                    builder.append(",");
            }
            builder.append("}");
        } else if (obj instanceof Iterable) {
            Iterator it = ((Iterable) obj).iterator();
            builder.append("[");
            while (it.hasNext()) {
                toJSONSub(it.next(), stack, builder);
                if (it.hasNext())
                    builder.append(",");
            }
            builder.append("]");
        } else if (obj instanceof Object[]) {
            builder.append("[");
            int len = Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                toJSONSub(Array.get(obj, i), stack, builder);
                if (i < len - 1)
                    builder.append(",");
            }
            builder.append("]");
        } else if (obj instanceof long[]) {
            long[] a = (long[]) obj;
            builder.append("[");
            for (int i = 0; i < a.length; i++) {
                builder.append(String.format("0x%016x", a[i]));
                if (i < a.length - 1)
                    builder.append(",");
            }
            builder.append("]");
        } else if (obj instanceof int[]) {
            int[] a = (int[]) obj;
            builder.append("[");
            for (int i = 0; i < a.length; i++) {
                builder.append(String.format("0x%08x", a[i]));
                if (i < a.length - 1)
                    builder.append(",");
            }
            builder.append("]");
        } else if (obj instanceof float[]) {
            float[] a = (float[]) obj;
            builder.append("[");
            for (int i = 0; i < a.length; i++) {
                builder.append(String.format("%.3f", a[i]));
                if (i < a.length - 1)
                    builder.append(",");
            }
            builder.append("]");
        } else if (obj instanceof double[]) {
            double[] a = (double[]) obj;
            builder.append("[");
            for (int i = 0; i < a.length; i++) {
                builder.append(String.format("%.6f", a[i]));
                if (i < a.length - 1)
                    builder.append(",");
            }
            builder.append("]");
        } else if (obj instanceof short[]) {
            short[] a = (short[]) obj;
            builder.append("[");
            for (int i = 0; i < a.length; i++) {
                builder.append(String.format("0x%04x", a[i]));
                if (i < a.length - 1)
                    builder.append(",");
            }
            builder.append("]");
        } else if (obj instanceof byte[]) {
            byte[] a = (byte[]) obj;
            builder.append("[");
            for (int i = 0; i < a.length; i++) {
                builder.append(String.format("0x%02x", a[i]));
                if (i < a.length - 1)
                    builder.append(",");
            }
            builder.append("]");
        } else if (obj instanceof boolean[]) {
            boolean[] a = (boolean[]) obj;
            builder.append("[");
            for (int i = 0; i < a.length; i++) {
                builder.append(a[i]);
                if (i < a.length - 1)
                    builder.append(",");
            }
            builder.append("]");
        } else if(obj.getClass().isEnum()) {
            builder.append(String.valueOf(obj));
        } else {
            builder.append("{");
            Method[] methods = obj.getClass().getMethods();
            List<Method> filteredMethods = new ArrayList<Method>();
            for (Method method : methods) {
                if (omitMethods.contains(method.getName()) || !isGetter(method))
                    continue;
                filteredMethods.add(method);
            }
            Iterator<Method> iterator = filteredMethods.iterator();
            while (iterator.hasNext()) {
                Method method = iterator.next();
                String name = toName(method);
                invoke(obj, stack, builder, method, name);
                if (iterator.hasNext())
                    builder.append(",");
            }
            builder.append("}");
        }

        stack.pop();
    }

    private static void invoke(Object obj, IntArrayList stack, StringBuilder builder, Method method, String name) {
        try {
            Object invoke = method.invoke(obj);
            builder.append('"');
            builder.append(name);
            builder.append("\":");
            if (invoke != null && primitive.contains(invoke.getClass()))
                builder.append(invoke);
            else
                toJSONSub(invoke, stack, builder);
        } catch (Exception e) {
        }
    }

    private static void escape(String invoke, StringBuilder sb) {
        char[] ch = invoke.toCharArray();
        for (char c : ch) {
            if (c < 0x20)
                sb.append(String.format("\\%02x", (int) c));
            else
                sb.append(c);
        }
    }

    private static String toName(Method method) {
        if (!isGetter(method))
            throw new IllegalArgumentException("Not a getter");

        char[] name = method.getName().toCharArray();
        int ind = name[0] == 'g' ? 3 : 2;
        name[ind] = Character.toLowerCase(name[ind]);
        return new String(name, ind, name.length - ind);
    }

    private static boolean isGetter(Method method) {
        if (!Modifier.isPublic(method.getModifiers()))
            return false;
        if (!method.getName().startsWith("get")
                && !(method.getName().startsWith("is") && method.getReturnType() == Boolean.TYPE))
            return false;
        if (method.getParameterTypes().length != 0)
            return false;
//        if (void.class.equals(method.getReturnType()))
//            return false;
        return true;
    }
}
