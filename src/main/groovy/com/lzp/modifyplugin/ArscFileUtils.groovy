package com.lzp.modifyplugin

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream;

class ArscFileUtils {
    static byte[] getArscFileContentFromApk(File apkFile) {
        ZipFile zipFile = new ZipFile(apkFile)
        ZipEntry arscEntry = zipFile.getEntry("resources.arsc")

        byte[] buffer = new byte[arscEntry.size]
        int len = zipFile.getInputStream(arscEntry).read(buffer)

        zipFile.close()
        return buffer
    }

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

    static replaceArscFile(File apkFile, File arscFile) {
        def zipFile = new ZipFile(apkFile)
        def entryContents = new byte[zipFile.size() - 1][]
        def entryNames = new String[zipFile.size() - 1]

        //把出了resources.arsc之外的文件读取出来保存到entryContents二维数组中
        def index = 0
        def entrys = zipFile.entries()
        while (entrys.hasMoreElements()) {
            def zipEntry = entrys.nextElement()

            if (zipEntry.name.equals(arscFile.name))
                continue

            entryNames[index] = zipEntry.name
            entryContents[index] = new byte[zipEntry.size]
            zipFile.getInputStream(zipEntry).read(entryContents[index])
            index++
        }

        def zos = new ZipOutputStream(new FileOutputStream(apkFile))
        for (int i = 0; i < zipFile.size() - 1; i++) {
            zos.putNextEntry(new ZipEntry(entryNames[i]))
            zos.write(entryContents[i])
            zos.closeEntry()
        }

        zos.putNextEntry(new ZipEntry(arscFile.name))
        def buffer = new byte[arscFile.size()]
        arscFile.withInputStream {
            zos.write(buffer, 0, it.read(buffer))
        }
        zos.closeEntry()
        zos.close()
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
