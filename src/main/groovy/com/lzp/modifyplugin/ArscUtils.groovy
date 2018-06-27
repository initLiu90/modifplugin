package com.lzp.modifyplugin

import java.nio.channels.FileChannel
import java.util.zip.ZipEntry
import java.util.zip.ZipFile;

/**
 * Created by lillian on 2018/6/24.
 */

public class ArscUtils {
    private static def LIBRARY_HEADER_SIZE = 0x0C
    private static def LIBRARY_ENTRY_SIZE = 260 // packageId(4), packageName(256)
    private static def LIBRARY_CHUNK_SIZE = 272 // ResTable_lib_header & ResTable_lib_entry

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
        def packageSizeOffset = packageChunkOffset + 4
        def packageSize = getResChunk_header_chunkSize(Utils.readFileContent(arscFile, packageChunkOffset, 8))

        src = Utils.readFileContent(arscFile, packageIdOffset, 4)
        def oldPackageId = Utils.byte2int(Utils.copyByte(src, 0, 4))
        def packageNameBytes = Utils.readFileContent(arscFile, packageIdOffset + 4, 2 * 128)
        Log.log("changeArscPackageId", "oldPackageId=" + oldPackageId)

        //get ResTable_typeSpec开始位置
        def packageKeyStrings = Utils.byte2int(Utils.readFileContent(arscFile, packageChunkOffset + resTable_package_header_size + 4 + 2 * 128 + 4 + 4, 4))
        def keyStringPoolChunkOffset = packageChunkOffset + packageKeyStrings
        src = Utils.readFileContent(arscFile, keyStringPoolChunkOffset, 8)
        def StringPool_header_header_size = getResChunk_header_chunkSize(src, 0)
        def resTypeOffset = keyStringPoolChunkOffset + StringPool_header_header_size

        if (oldPackageId == 127 && oldPackageId != packageId) {
            def newIdBytes = Utils.int2ByteArray(packageId)
            changeArscFileContent(arscFile, newIdBytes, packageIdOffset, 4)
//            addLibraryTable(arscFile, newIdBytes, packageNameBytes, resTypeOffset)
//            modifyTableSize(arscFile, packageSizeOffset, packageSize + LIBRARY_CHUNK_SIZE)
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

    private static addLibraryTable(File arscFile, byte[] newPackageId, byte[] newPackageName, int start) {
        RandomAccessFile sRaFile = new RandomAccessFile(arscFile, 'rw')
        int slenght = sRaFile.length()
        FileChannel sChannel = sRaFile.channel

        File tmpFile = new File(arscFile.getParent(), "${arscFile.name}~")
        RandomAccessFile tRaFile = new RandomAccessFile(tmpFile, 'rw')
        FileChannel tChannel = tRaFile.channel

        sChannel.transferTo(start, slenght - start, tChannel)
        sChannel.truncate(start)

        sRaFile.seek(start)
        //write ResTable_lib_header.ResChunk_header
        sRaFile.write(Utils.short2ByteArray(0x0203))//type
        sRaFile.write(Utils.short2ByteArray(LIBRARY_HEADER_SIZE))//headerSize
        sRaFile.write(Utils.int2ByteArray(LIBRARY_CHUNK_SIZE))
        sRaFile.write(Utils.int2ByteArray(1))

        //write ResTable_lib_entry
        sRaFile.write(newPackageId)
        sRaFile.write(newPackageName)

        long newPos = sRaFile.getFilePointer()
        tChannel.position(0L)
        sChannel.transferFrom(tChannel, newPos, slenght - start)

        sRaFile.close()
        tRaFile.close()
        tmpFile.delete()
    }

    private static modifyTableSize(File arscFile, int start, int newSize) {
        RandomAccessFile raFile = new RandomAccessFile(arscFile, 'rw')
        raFile.seek(start)
        raFile.write(Utils.int2ByteArray(newSize))
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
