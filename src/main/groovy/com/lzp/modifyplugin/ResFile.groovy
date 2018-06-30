package com.lzp.modifyplugin

import org.gradle.internal.impldep.com.beust.jcommander.ParameterException

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel;

class ResFile {
    protected File mFile
    private RandomAccessFile mRaFile

    def ResFile(def file) {
        if (file instanceof String) {
            mFile = new File(file)
        } else if (file instanceof File) {
            mFile = file
        } else {
            throw new ParameterException("must file path or file instance")
        }
        mRaFile = new RandomAccessFile(mFile, 'rw')
    }

    protected def readResChunk_header() {
        def header = [:]
        header.type = readShort()
        header.headerSize = readShort()
        header.size = readInt()
        return header
    }

    protected def writeInt(int value) {
        def data = Utils.int2ByteArray(value)
        return writeBytes(data)
    }

    protected def writeBytes(byte[] value) {
        mRaFile.write(value)
        return mRaFile.filePointer
    }

    /**
     * 从当前文件指针处开始读取
     * @param len 读取的字节数
     * @return
     */
    protected def readBytes(long len) {
        byte[] buffer = new byte[len]
        mRaFile.read(buffer)
        return buffer
    }

    /**
     * @param start 开始读取的位置
     * @param len 读取的字节数
     * @return 读取到的内容
     */
    protected def readBytes(long start, long len) {
        byte[] buffer = new byte[len]
        mRaFile.seek(start)
        mRaFile.read(buffer, 0, len)
        return buffer
    }

    protected def readInt() {
        byte[] buffer = new byte[4]
        mRaFile.read(buffer)
        return Utils.byte2int(buffer)
    }

    protected def readInt(long start) {
        mRaFile.seek(start)
        byte[] buffer = new byte[4]
        mRaFile.read(buffer)
        return Utils.byte2int(buffer)
    }

    protected def readShort() {
        byte[] buffer = new byte[2]
        mRaFile.read(buffer)
        return Utils.byte2Short(buffer)
    }

    protected def readShort(long start) {
        mRaFile.seek(start)
        byte[] buffer = new byte[2]
        mRaFile.read(buffer)
        return Utils.byte2Short(buffer)
    }

    protected def readByte() {
        return mRaFile.readByte()
    }

    protected def readByte(long start) {
        seek(seek())
        return mRaFile.readByte()
    }

    protected def seek(long pos) {
        mRaFile.seek(pos)
    }

    protected def skip(int n) {
        mRaFile.skipBytes(n)
    }

    protected def getFilePointer() {
        return mRaFile.getFilePointer()
    }

    protected def insert(long pos, byte[] data) {
        FileChannel sChannel = mRaFile.channel
        int sLength = mRaFile.length()

        //创建一个临时文件
        File tmpFile = new File(mFile.getParent(), "${mFile.name}~")
        RandomAccessFile tRaFile = new RandomAccessFile(tmpFile, 'rw')
        FileChannel tChannel = tRaFile.channel

        //将pos之后的内容存储到临时文件中
        sChannel.transferTo(pos, sLength - pos, tChannel)
        //截取0-pos之间的内容
        sChannel.truncate(pos)

        mRaFile.seek(pos)
        writeBytes(data)

        long newPos = mRaFile.getFilePointer()
        tChannel.position(0L)
        sChannel.transferFrom(tChannel, newPos, sLength - pos)

        tChannel.close()
        tRaFile.close()
        tmpFile.delete()
    }

    def close() {
        if (mRaFile != null) {
            mRaFile.close()
        }
    }
}
