package com.example.inakov.beaconobserver;

/**
 * Created by inakov on 16-2-29.
 */
public final class ParserUtils {

    public static long decodeHalfUuid(byte[] data, int start) {
        return (unsignedByteToLong(data[start]) << 56) + (unsignedByteToLong(data[start + 1]) << 48) + (unsignedByteToLong(data[start + 2]) << 40) + (unsignedByteToLong(data[start + 3]) << 32) + (unsignedByteToLong(data[start + 4]) << 24) + (unsignedByteToLong(data[start + 5]) << 16) + (unsignedByteToLong(data[start + 6]) << 8) + unsignedByteToLong(data[start + 7]);
    }

    public static int decodeUint16BigEndian(byte[] data, int start) {
        int b1 = data[start] & 255;
        int b2 = data[start + 1] & 255;
        return b1 | b2 << 8;
    }

    public static int decodeUint16LittleEndian(byte[] data, int start) {
        int b1 = data[start] & 255;
        int b2 = data[start + 1] & 255;
        return b1 << 8 | b2;
    }

    public static int unsignedByteToInt(byte b) {
        return b & 255;
    }

    public static long unsignedByteToLong(byte b) {
        return (long)(b & 255);
    }
}
