package com.lzp.modifyplugin

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream;

class ApkFileUtils {
    static byte[] getArscFileContentFromApk(File apkFile) {
        ZipFile zipFile = new ZipFile(apkFile)
        ZipEntry arscEntry = zipFile.getEntry("resources.arsc")

        byte[] buffer = new byte[arscEntry.size]
        int len = zipFile.getInputStream(arscEntry).read(buffer)

        zipFile.close()
        return buffer
    }

    static byte[] getAndroidManifestContentFromApk(File apkFile) {
        ZipFile zipFile = new ZipFile(apkFile)
        ZipEntry amEntry = zipFile.getEntry("AndroidManifest.xml")

        byte[] buffer = new byte[amEntry.size]
        zipFile.getInputStream(amEntry).read(buffer)

        zipFile.close()
        return buffer
    }

    static replaceArscFileInApk(File apkFile, File arscFile) {
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
}
