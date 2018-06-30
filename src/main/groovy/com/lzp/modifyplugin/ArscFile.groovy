package com.lzp.modifyplugin

import java.nio.ByteBuffer

class ArscFile extends ResFile {
    private int mOldPackageId = 127//0x7f

    def ArscFile(def file) {
        super(file)
    }

    def modifyArsc(int packageId) {
        //读取前8个byte，ResTable_header.ResChunk_header
        def resChunk_header = readResChunk_header()
        def resStringPoolChunkOffset = resChunk_header.headerSize
        def tableSize = resChunk_header.size

        //读取ResStringPool_header.ResChunk_header,8Byte
        seek(resStringPoolChunkOffset)
        resChunk_header = readResChunk_header()
        def packageChunkOffset = resStringPoolChunkOffset + resChunk_header.size

        //读取ResTable_package.ResChunk_header
        seek(packageChunkOffset)
        def package_resChunk_header = readResChunk_header()
        def packageSizeOffset = getFilePointer() - 4
        def packageIdOffset = getFilePointer()
        skip(4)//skip id
        def packageNameBytes = readBytes(2 * 128)

        //get ResTable_typeSpec开始位置
        skip(4 + 4)//skip typeStrings,lastPublicType
        def keyStrings = readInt()
        def keStringPoolOffset = packageChunkOffset + keyStrings
        seek(keStringPoolOffset)
        resChunk_header = readResChunk_header()
        def typeOffset = keStringPoolOffset + resChunk_header.size

        //stpe:1
        changePackageId(packageIdOffset, packageId)
        //step:2
        changeEntryId(typeOffset, packageId, tableSize)
        //step:3
        addLibraryTable(packageId, packageNameBytes, typeOffset)
        //step:4
        changeTableAndPackeageSize(4, tableSize + Constants.LIBRARY_CHUNK_SIZE, packageSizeOffset, package_resChunk_header.size + Constants.LIBRARY_CHUNK_SIZE)
    }

    private
    def changeTableAndPackeageSize(long tableSizePos, int newTableSize, long packageSizePos, int newPackageSize) {
        seek(tableSizePos)
        writeInt(newTableSize)

        seek(packageSizePos)
        writeInt(newPackageSize)
    }

    /**
     * 添加ResTable_lib_header和ResTable_lib_entry
     * @param packageId
     * @param packageNameBytes
     * @param typeOffset
     * @return
     */
    private def addLibraryTable(int packageId, byte[] packageNameBytes, long typeOffset) {
        ByteBuffer buffer = ByteBuffer.allocate(272)

        buffer.put(Utils.short2ByteArray(0x0203))//type
        buffer.put(Utils.short2ByteArray(Constants.LIBRARY_HEADER_SIZE))
        buffer.put(Utils.int2ByteArray(Constants.LIBRARY_CHUNK_SIZE))
        buffer.put(Utils.int2ByteArray(1))

        buffer.put(Utils.int2ByteArray(packageId))
        buffer.put(packageNameBytes)

        insert(typeOffset, buffer.array())
    }

    /**
     * 更换读取ResTable_package中的packageid
     * @param packageId
     * @return 返回修改前的packageid
     */
    private def changePackageId(def pos, def packageId) {
        seek(pos)
        def id = readInt()
        if (id != packageId) {
            seek(pos)
            writeInt(packageId)
        }
        return 0
    }

    /**
     * ResTable_entry中Res_value、及ResTable_map_entry的资源id
     * @param pos 开始位置
     * @param packageId
     * @param tableSize 资源表大小
     */
    private def changeEntryId(int pos, int packageId, int tableSize) {
        seek(pos)
        while (getFilePointer() < tableSize) {
            def type = readResTableType()
            //此时文件指针在ResTable_type或ResTable_typeSpec开始处

            if (!type.isSpec) {//ResTable_type
                parseResTable_Type(type, packageId, tableSize)
            } else {//ResTable_typeSpec
                //跳过ResTable_type
                skip(type.header.size)
            }
        }
    }

    private def parseResTable_Type(def type, def packageId, def tableSize) {
        def startPos = getFilePointer()
        //取到偏移数组的开始位置
        def tmp = (startPos + type.entriesStart) - (type.entryCount * 4)
        seek(tmp)

        def offsetAry = []
        for (int i = 0; i < type.entryCount; i++) {
            offsetAry[i] = readInt()
        }

        //这里开始解析ResTable_entry
        def final firstEntryOffset = startPos + type.entriesStart
        for (int i = 0; i < type.entryCount; i++) {
            if (offsetAry[i] == -1) continue

            def entryOffset = firstEntryOffset + offsetAry[i]
            seek(entryOffset)
            if (tableSize - entryOffset < 16) break

            readShort()//ResTable_entry.size
            def entryFlags = readShort()//ResTable_entry.flags
            readInt()//ResTable_entry.key
            if (entryFlags == 0) {//Res_value
                skip(4)
                changeResId(packageId)//change data id
            } else if (entryFlags & Constants.ResTable_entry_FLAG_COMPLEX) {
                changeResId(packageId)//change parent id
                def count = readInt()
                //根据资源表的结构ResTable_map_entry大小应该为16，这里为什么只跳过前8个字节？？？？？？
                for (int j = 0; j < count; j++) {
                    changeResId(packageId)//change name id
                    skip(4)
                    changeResId(packageId)//change data id
                }
            }
        }
        seek(startPos + type.header.size)
    }

    private void changeResId(int newPackId) {
        int pos = getFilePointer()
        def id = readInt()
        if (id >> 24 == mOldPackageId) {
            id = ((newPackId << 24) | (id & 0x00ffffff))
            seek(pos)
            writeInt(id)
        }
    }

    private def readResTableType() {
        int pos = getFilePointer()
        def type = [:]
        type.header = readResChunk_header()
        if (type.header.type == Constants.RES_TABLE_TYPE_SPEC_TYPE) {
            type.isSpec = true
            type.entryCount = readInt(getFilePointer() + 4)
        } else if (type.header.type == Constants.RES_TABLE_TYPE_TYPE) {
            type.isSpec = false
            type.entryCount = readInt(getFilePointer() + 4)
            type.entriesStart = readInt()
        }
        //回到原位置
        seek(pos)
        return type
    }
}
