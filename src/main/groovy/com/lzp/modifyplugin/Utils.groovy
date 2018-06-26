package com.lzp.modifyplugin;

final class Utils {
    static byte[] copyByte(byte[] src, int start, int len) {
        if (src == null) return null;

        if (start > src.length) return null;

        if ((start + len) > src.length) return null;

        if (start < 0) return null;

        if (len <= 0) return null;

        byte[] resultByte = new byte[len];
        for (int i = 0; i < len; i++) {
            resultByte[i] = src[i + start];
        }
        return resultByte;
    }


    static short byte2Short(byte[] b) {
        short s = 0;
        short s0 = (short) (b[0] & 0xff);
        short s1 = (short) (b[1] & 0xff);
        s1 <<= 8;
        s = (short) (s0 | s1);
        return s;
    }

    static int byte2int(byte[] b) {
        int targets = (b[0] & 0xff) | ((b[1] << 8) & 0xff00) | ((b[2] << 24) >>> 8) | (b[3] << 24);
        return targets;
    }

    /**
     * 网络字节序
     * @param value
     * @return
     */
    static byte[] int2ByteArray(int value) {
        byte[] result = new byte[4];

        result[0] = (byte) (value);
        result[1] = (byte) (value >> 8);
        result[2] = (byte) (value >> 16);
        result[3] = (byte) (value >> 24);

        return result;
    }

    static String packageName2Path(String packageName) {
        return packageName.replace('.', File.separator)
    }

    static readFileContent(File file, int start, int len) {
        byte[] buffer = new byte[len]
        RandomAccessFile raFile = new RandomAccessFile(file, 'r')
        raFile.seek(start)
        raFile.read(buffer, 0, len)
        raFile.close()
        return buffer
    }
}
