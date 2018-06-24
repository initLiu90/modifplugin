package com.lzp.modifyplugin;

/**
 * Created by lillian on 2018/6/24.
 */

public class ArscUtils {
    /**
     *
     * @param src resources.arsc file conent
     * @param packageId new package id
     * @return resources.arsc file content with new package id
     */
    static byte[] changeArscPackageId(byte[] src, int packageId) {
        def resStringPoolChunkOffset = getResChunk_header_headerSize(src, 0)
        def packageChunkOffset = resStringPoolChunkOffset + getResChunk_header_chunkSize(src, resStringPoolChunkOffset)

        def resTable_package_header_size = 8

        def packageIdOffset = packageChunkOffset + resTable_package_header_size

        def oldPackageId = Utils.byte2int(Utils.copyByte(src, packageIdOffset, 4))

        if (oldPackageId != packageId) {
            def newSrc = new byte[src.length]
            def newIdBytes = Utils.int2ByteArray(packageId)

            for (def i = 0; i < src.length; i++) {
                if (i >= packageIdOffset && i < packageIdOffset + 4) {
                    int index = i - packageIdOffset;
                    newSrc[i] = newIdBytes[index];
                } else {
                    newSrc[i] = src[i];
                }
            }
            return newSrc
        }
        return src
    }

    static generateNewArscFile(String path, byte[] src) {
        def arscFile = new File(path, "resources.arsc")
        arscFile.withOutputStream {
            it.write(src)
            it.flush()
        }
        return arscFile
    }

    private static getResChunk_header_headerSize(byte[] src, int start) {
        def headerSizeByte = Utils.copyByte(src, start + 2, 2)
        def headerSize = Utils.byte2Short(headerSizeByte)
        return headerSize
    }

    private static getResChunk_header_chunkSize(byte[] src, int start) {
        def tableSizeByte = Utils.copyByte(src, start + 4, 4)
        def size = Utils.byte2int(tableSizeByte)
        return size
    }
}
