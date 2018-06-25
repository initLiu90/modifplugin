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

    static byte[] getEntryContentFromApk(File apkFile, ZipEntry zipEntry) {
        ZipFile zipFile = new ZipFile(apkFile)

        byte[] buffer = new byte[zipEntry.size]
        zipFile.getInputStream(zipEntry).read(buffer)

        zipFile.close()
        return buffer
    }

    static getXmlFilesInApk(File apkFile) {
        def xmlEntrys = new ArrayList()

        def zipFile = new ZipFile(apkFile)
        def entrys = zipFile.entries()

        while (entrys.hasMoreElements()) {
            def zipEntry = entrys.nextElement()
            if (zipEntry.name.endsWith(".xml")) {
                xmlEntrys.add(zipEntry)
            }
        }
        return xmlEntrys.toArray()
    }

    static replaceFilesInApk(File apkFile, Map<String, File> xmlFiles, File arscFile) {
        def zipFile = new ZipFile(apkFile)

        final int skiCounts = xmlFiles.size() + 1
        final int remainEntryCounts = zipFile.size() - skiCounts
//        println("####### replaceFilesInApk entryCounts=" + remainEntryCounts)

        def entryContents = new byte[remainEntryCounts][]
        def entryNames = new String[remainEntryCounts]

        //把出了resources.arsc之外的文件读取出来保存到entryContents二维数组中
        def index = 0
        def entrys = zipFile.entries()
        while (entrys.hasMoreElements()) {
            def zipEntry = entrys.nextElement()

//            println("### index=" + index + "  " + zipEntry.name)
            if (zipEntry.name.equals(arscFile.name)) {
                continue
            }

            def skip = false
            for(Map.Entry<String,File> entry:xmlFiles.entrySet()){
                if(entry.key.equals(zipEntry.name)){
                    skip = true
                    break
                }
            }

            if (skip) continue

//            println("### index2=" + index + "   " + zipEntry.name)

            entryNames[index] = zipEntry.name
            entryContents[index] = new byte[zipEntry.size]
            zipFile.getInputStream(zipEntry).read(entryContents[index])
            index++
        }

//        println("####################")
        def zos = new ZipOutputStream(new FileOutputStream(apkFile))
        for (int i = 0; i < remainEntryCounts; i++) {
            zos.putNextEntry(new ZipEntry(entryNames[i]))
            zos.write(entryContents[i])
            zos.closeEntry()
        }

        //replace arscFile
        zos.putNextEntry(new ZipEntry(arscFile.name))
        def buffer = new byte[arscFile.size()]
        arscFile.withInputStream {
            zos.write(buffer, 0, it.read(buffer))
        }
        zos.closeEntry()

        //replace xml files
        xmlFiles.each { xmlFileEntry ->
            zos.putNextEntry(new ZipEntry(xmlFileEntry.key))
            buffer = new byte[xmlFileEntry.value.size()]
            xmlFileEntry.value.withInputStream {
                zos.write(buffer, 0, it.read(buffer))
            }
            zos.closeEntry()
        }

        zos.close()
    }
}
