package com.lzp.modifyplugin

import java.nio.Buffer
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

    public static int RES_TABLE_TYPE_TYPE = 0x0201;
    public static int RES_TABLE_TYPE_SPEC_TYPE = 0x0202;
    /**
     *
     * @param apkFile
     * @param packageId new package id
     * @return resources.arsc file content with new package id
     */
    static def changeArscPackageId(File arscFile, int packageId) {
        //读取前8个byte，ResTable_header.ResChunk_header
        byte[] src = Utils.readFileContent(arscFile, 0, 8)
        def resStringPoolChunkOffset = getResChunk_header_headerSize(src, 0)
        def tableSize = getResChunk_header_chunkSize(src, 0)

        //读取ResStringPool_header.ResChunk_header,8Byte
        src = Utils.readFileContent(arscFile, resStringPoolChunkOffset, 8)
        def packageChunkOffset = resStringPoolChunkOffset + getResChunk_header_chunkSize(src, 0)

        def resTable_package_header_size = 8

        //读取package.header
        src = Utils.readFileContent(arscFile, packageChunkOffset, 8)
//        Log.log("package header bytes", src[4] + ' ' + src[5] + ' ' + src[6] + ' ' + src[7])
        def packageIdOffset = packageChunkOffset + resTable_package_header_size
        def packageSizeOffset = packageChunkOffset + 4
        def packageSize = getResChunk_header_chunkSize(src, 0)
//        Log.log("packageSizeOffset", packageSizeOffset)

        src = Utils.readFileContent(arscFile, packageIdOffset, 4)
        def oldPackageId = Utils.byte2int(src)
        Log.log("changeArscPackageId", "oldPackageId=" + oldPackageId)

        def packageNameBytes = Utils.readFileContent(arscFile, packageIdOffset + 4, 2 * 128)
//        Log.log("packageName", new String(packageNameBytes))

        //get ResTable_typeSpec开始位置
        def packageKeyStrings = Utils.byte2int(Utils.readFileContent(arscFile, packageChunkOffset + resTable_package_header_size + 4 + 2 * 128 + 4 + 4, 4))
        def keyStringPoolChunkOffset = packageChunkOffset + packageKeyStrings
        src = Utils.readFileContent(arscFile, keyStringPoolChunkOffset, 8)
        def StringPool_header_header_size = getResChunk_header_chunkSize(src, 0)
        def resTypeOffset = keyStringPoolChunkOffset + StringPool_header_header_size
//        Log.log("resTypeOffset", resTypeOffset)

        if (oldPackageId == 127 && oldPackageId != packageId) {
            def newIdBytes = Utils.int2ByteArray(packageId)
            changePackageId(arscFile, newIdBytes, packageIdOffset, 4)
            changeResValuePackageId(arscFile, packageId, resTypeOffset, tableSize)
            addLibraryTable(arscFile, newIdBytes, packageNameBytes, resTypeOffset)
            modifyTableSize(arscFile, 4, tableSize + LIBRARY_CHUNK_SIZE, packageSizeOffset, packageSize + LIBRARY_CHUNK_SIZE)
        }
    }

    /**
     *  修改ResTable_package中的packageid
     * @param arscFile
     * @param newPackageId 新的packageId
     * @param start packageId的开始位置
     * @param len长度
     * @return
     */
    private
    static def changePackageId(File arscFile, byte[] newPackageId, int start, int len) {
        RandomAccessFile raFile = new RandomAccessFile(arscFile, 'rw')
        raFile.seek(start)
        Log.log("changePackageId", raFile.getFilePointer())
        raFile.write(newPackageId)
        raFile.close()
    }

    /**
     * 修改Res_value.data的packageId
     * @param arscFile
     * @param newPackageId
     * @param start
     */
    private static
    def changeResValuePackageId(File arscFile, int newPackageId, int start, int tableSize) {
        while (start < tableSize) {
            def type = readResTableType(arscFile, start)
            if (type.isConfig) {//ResTable_type
                start = parseResTable_Type(arscFile, type, newPackageId, start, tableSize)
            } else {
                start += type.header.size
            }
        }
    }

    private static def parseResTable_Type(
            def arscFile, def type, def newPackId, def start, def tableSize) {

        def tmp = (start + type.entriesStart) - (type.entryCount * 4)
        def entriesOffsets = []
        for (int i = 0; i < type.entryCount; i++) {
            entriesOffsets[i] = Utils.byte2int(Utils.readFileContent(arscFile, tmp, 4))
            tmp += 4
        }

        //这里开始解析后面对应的ResEntry和ResValue
        def entryAryOffsetStart = start + type.entriesStart
        for (int i = 0; i < type.entryCount; i++) {
            if (entriesOffsets[i] == -1) continue

            def entryAryOffset = entryAryOffsetStart + entriesOffsets[i]
            Log.log("1111", "entryAryOffset=" + entryAryOffset + "," + entriesOffsets[i])

            if (tableSize - entryAryOffset < 16) break

            def flags = Utils.byte2Short(Utils.readFileContent(arscFile, entryAryOffset + 2, 2))
            entryAryOffset += 8
            Log.log("222", "entryAryOffset=" + entryAryOffset)
            if (flags == 0) {//Res_value
                def buffer = Utils.readFileContent(arscFile, entryAryOffset + 4, 4)
                def valueId = Utils.byte2int(Utils.readFileContent(arscFile, entryAryOffset + 4, 4))
                if (valueId >> 24 == 0x7f) {
                    Log.log("resid4", "valueId=0x" + Integer.toHexString(valueId) + "," + buffer[0] + " " + buffer[1] + " " + buffer[2] + " " + buffer[3])
                    valueId = ((newPackId << 24) | (valueId & 0x00ffffff))
                    changePackageId(arscFile, Utils.int2ByteArray(valueId), entryAryOffset + 4, 4)
                }
                entryAryOffset += 8
            } else if (flags & 0x0001) {//ResTable_map_entry
                def buffer = Utils.readFileContent(arscFile, entryAryOffset, 4)
                def parentId = Utils.byte2int(Utils.readFileContent(arscFile, entryAryOffset, 4))
                if (parentId >> 24 == 0x7f) {
                    Log.log("resid1", "parentId=0x" + Integer.toHexString(parentId) + "," + buffer[0] + " " + buffer[1] + " " + buffer[2] + " " + buffer[3])
                    parentId = ((newPackId << 24) | (parentId & 0x00ffffff))
                    changePackageId(arscFile, Utils.int2ByteArray(parentId), entryAryOffset, 4)
                }

                def count = Utils.byte2int(Utils.readFileContent(arscFile, entryAryOffset + 4, 4))
                def mapEntrySize = Utils.byte2Short(Utils.readFileContent(arscFile, entryAryOffset + 4 + 4, 2))
                entryAryOffset += 8
                Log.log("3333", "entryAryOffset=" + entryAryOffset)

                for (int j = 0; j < count; j++) {
                    buffer = Utils.readFileContent(arscFile, entryAryOffset, 4)
                    def mapNameId = Utils.byte2int(Utils.readFileContent(arscFile, entryAryOffset, 4))
                    if (mapNameId >> 24 == 0x7f) {
                        Log.log("resid2", "mapNameId=0x" + Integer.toHexString(mapNameId) + "," + buffer[0] + " " + buffer[1] + " " + buffer[2] + " " + buffer[3])
                        mapNameId = ((newPackId << 24) | (mapNameId & 0x00ffffff))
                        changePackageId(arscFile, Utils.int2ByteArray(mapNameId), entryAryOffset, 4)
                    }

                    buffer = Utils.readFileContent(arscFile, entryAryOffset + 4 + 4, 4)
                    def mapValueId = Utils.byte2int(Utils.readFileContent(arscFile, entryAryOffset + 4 + 4, 4))
                    if (mapValueId >> 24 == 0x7f) {
                        Log.log("resid3", "mapValueId=0x" + Integer.toHexString(mapValueId) + "," + buffer[0] + " " + buffer[1] + " " + buffer[2] + " " + buffer[3])
                        mapValueId = ((newPackId << 24) | (mapValueId & 0x00ffffff))
                        changePackageId(arscFile, Utils.int2ByteArray(mapValueId), entryAryOffset + 4 + 4, 4)
                    }

                    entryAryOffset += 12
                }
            }
        }

        start += type.header.size
        return start
    }

    private static def readResTableType(File arscFile, int start) {
        def type = [:]
        def src = Utils.readFileContent(arscFile, start, 8)
        def header = getResChunk_header(src, 0)
        type.header = header
        if (header.type == RES_TABLE_TYPE_SPEC_TYPE) {
            type.isConfig = false
            src = Utils.readFileContent(arscFile, start + 8 + 4, 4)
            type.entryCount = Utils.byte2int(src)
        } else if (header.type == RES_TABLE_TYPE_TYPE) {
            type.isConfig = true
            src = Utils.readFileContent(arscFile, start + 8 + 4, 8)
            type.entryCount = Utils.byte2int(Utils.copyByte(src, 0, 4))
            type.entriesStart = Utils.byte2int(Utils.copyByte(src, 4, 4))
        }
        return type
    }

    private
    static
    def addLibraryTable(File arscFile, byte[] newPackageId, byte[] newPackageName, int start) {
        RandomAccessFile sRaFile = new RandomAccessFile(arscFile, 'rw')
        int slenght = sRaFile.length()
        FileChannel sChannel = sRaFile.channel

        File tmpFile = new File(arscFile.getParent(), "${arscFile.name}~")
        RandomAccessFile tRaFile = new RandomAccessFile(tmpFile, 'rw')
        FileChannel tChannel = tRaFile.channel

        sChannel.transferTo(start, slenght - start, tChannel)
        sChannel.truncate(start)

        Log.log("start", start)
        sRaFile.seek(start)
        //write ResTable_lib_header.ResChunk_header
        sRaFile.write(Utils.short2ByteArray(0x0203))//type
        sRaFile.write(Utils.short2ByteArray(LIBRARY_HEADER_SIZE))//headerSize
        sRaFile.write(Utils.int2ByteArray(LIBRARY_CHUNK_SIZE))
        sRaFile.write(Utils.int2ByteArray(1))

        //write ResTable_lib_entry
        sRaFile.write(newPackageId)
        sRaFile.write(newPackageName)
        Log.log("newPackageName size=", newPackageName.length)
        long newPos = sRaFile.getFilePointer()
        Log.log("newPos", newPos)
        tChannel.position(0L)
        sChannel.transferFrom(tChannel, newPos, slenght - start)

        sChannel.close()
        tChannel.close()
        sRaFile.close()
        tRaFile.close()
        tmpFile.delete()
    }

    private
    static
    def modifyTableSize(File arscFile, int tableSizeStart, int newTableSize, int packageSizeStart, int newPackageSize) {
        RandomAccessFile raFile = new RandomAccessFile(arscFile, 'rw')
        //修改ResTable_header size
        raFile.seek(tableSizeStart)
        raFile.write(Utils.int2ByteArray(newTableSize))

        //修改packagechunk size
        raFile.seek(packageSizeStart)
        raFile.write(Utils.int2ByteArray(newPackageSize))
        raFile.close()
    }


    private static def getResChunk_header_headerSize(byte[] src, int start) {
        def headerSizeByte = Utils.copyByte(src, start + 2, 2)
        def headerSize = Utils.byte2Short(headerSizeByte)
        return headerSize
    }

    private static def getResChunk_header_chunkSize(byte[] src, int start) {
        def tableSizeByte = Utils.copyByte(src, start + 4, 4)
        def size = Utils.byte2int(tableSizeByte)
        return size
    }

    private static def getResChunk_header(byte[] src, int start) {
        def header = [:]
        header.type = Utils.byte2Short(Utils.copyByte(src, start, 2))
        header.headerSize = Utils.byte2Short(Utils.copyByte(src, start + 2, 2))
        header.size = Utils.byte2int(Utils.copyByte(src, start + 4, 4))
        return header
    }
}
