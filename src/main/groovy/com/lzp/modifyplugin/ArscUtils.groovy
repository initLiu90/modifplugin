package com.lzp.modifyplugin

import java.util.zip.ZipEntry
import java.util.zip.ZipFile;

/**
 * Created by lillian on 2018/6/24.
 */

public class ArscUtils {
    /**
     *
     * @param apkFile
     * @param packageId new package id
     * @return resources.arsc file content with new package id
     */
    static changeArscPackageId(File arscFile, int packageId) {
        //读取前8个byte，ResTable_header.ResChunk_header
        byte[] src = Utils.readFileContent(arscFile, 0, 8)
        def resStringPoolChunkOffset = getResChunk_header_headerSize(src, 0)

        //读取ResStringPool_header.ResChunk_header,8Byte
        src = Utils.readFileContent(arscFile, resStringPoolChunkOffset, 8)
        def packageChunkOffset = resStringPoolChunkOffset + getResChunk_header_chunkSize(src, 0)

        def resTable_package_header_size = 8

        def packageIdOffset = packageChunkOffset + resTable_package_header_size

        src = Utils.readFileContent(arscFile, packageIdOffset, 4)
        def oldPackageId = Utils.byte2int(Utils.copyByte(src, 0, 4))
        Log.log("changeArscPackageId", "oldPackageId=" + oldPackageId)

        if (oldPackageId == 127 && oldPackageId != packageId) {
            def newIdBytes = Utils.int2ByteArray(packageId)
            changeArscFileContent(arscFile, newIdBytes, packageIdOffset, 4)
        }
    }

    /**
     *
     * @param arscFile
     * @param newPackageId 新的packageId
     * @param start packageId的开始位置
     * @param len长度
     * @return
     */
    private static changeArscFileContent(File arscFile, byte[] newPackageId, int start, int len) {
        RandomAccessFile raFile = new RandomAccessFile(arscFile, 'rw')
        raFile.seek(start)
        raFile.write(newPackageId)
        raFile.close()
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
