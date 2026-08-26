package cl.javier.salaremote.net;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class Proto {
    private Proto() {}

    static byte[] varint(long value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((value & ~0x7FL) != 0) {
            out.write((int)((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        out.write((int)value);
        return out.toByteArray();
    }

    static byte[] fieldVarint(int field, long value) {
        return concat(varint(((long)field << 3)), varint(value));
    }

    static byte[] fieldBytes(int field, byte[] data) {
        return concat(varint(((long)field << 3) | 2), varint(data.length), data);
    }

    static byte[] fieldString(int field, String s) {
        return fieldBytes(field, s.getBytes(StandardCharsets.UTF_8));
    }

    static byte[] concat(byte[]... parts) {
        int size = 0;
        for (byte[] p : parts) size += p.length;
        byte[] r = new byte[size];
        int off = 0;
        for (byte[] p : parts) { System.arraycopy(p, 0, r, off, p.length); off += p.length; }
        return r;
    }

    static void writeFrame(OutputStream out, byte[] payload) throws IOException {
        out.write(varint(payload.length));
        out.write(payload);
        out.flush();
    }

    static byte[] readFrame(InputStream in) throws IOException {
        int len = (int) readVarint(in);
        if (len < 0 || len > 1024 * 1024) throw new IOException("Invalid frame size: " + len);
        byte[] data = in.readNBytes(len);
        if (data.length != len) throw new EOFException("Unexpected end of stream");
        return data;
    }

    static long readVarint(InputStream in) throws IOException {
        long result = 0;
        int shift = 0;
        while (shift < 64) {
            int b = in.read();
            if (b < 0) throw new EOFException();
            result |= (long)(b & 0x7F) << shift;
            if ((b & 0x80) == 0) return result;
            shift += 7;
        }
        throw new IOException("Malformed varint");
    }

    static Map<Integer, List<Value>> parse(byte[] data) throws IOException {
        Map<Integer, List<Value>> map = new LinkedHashMap<>();
        int[] pos = {0};
        while (pos[0] < data.length) {
            long tag = readVarint(data, pos);
            int field = (int)(tag >>> 3);
            int wire = (int)(tag & 7);
            Value value;
            if (wire == 0) value = new Value(wire, readVarint(data, pos), null);
            else if (wire == 2) {
                int len = (int)readVarint(data, pos);
                if (len < 0 || pos[0] + len > data.length) throw new IOException("Bad length");
                byte[] b = Arrays.copyOfRange(data, pos[0], pos[0] + len);
                pos[0] += len;
                value = new Value(wire, 0, b);
            } else if (wire == 5) {
                pos[0] += 4; continue;
            } else if (wire == 1) {
                pos[0] += 8; continue;
            } else throw new IOException("Unsupported protobuf wire type " + wire);
            map.computeIfAbsent(field, k -> new ArrayList<>()).add(value);
        }
        return map;
    }

    static Value first(Map<Integer, List<Value>> m, int field) {
        List<Value> v = m.get(field); return v == null || v.isEmpty() ? null : v.get(0);
    }

    private static long readVarint(byte[] d, int[] pos) throws IOException {
        long result = 0; int shift = 0;
        while (shift < 64 && pos[0] < d.length) {
            int b = d[pos[0]++] & 0xff;
            result |= (long)(b & 0x7f) << shift;
            if ((b & 0x80) == 0) return result;
            shift += 7;
        }
        throw new IOException("Malformed varint");
    }

    static final class Value {
        final int wire; final long number; final byte[] bytes;
        Value(int wire, long number, byte[] bytes) { this.wire = wire; this.number = number; this.bytes = bytes; }
    }
}
