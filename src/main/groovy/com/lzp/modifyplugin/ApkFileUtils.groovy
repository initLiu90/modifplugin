package com.lzp.modifyplugin

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream;

class ApkFileUtils {
    static extractFilesFromApk(File apkFile){
        def files = [:]

        def zipFile = new ZipFile(apkFile)
        def entrys = zipFile.entries()

        while (entrys.hasMoreElements()) {
            def zipEntry = entrys.nextElement()
            def file = new File(apkFile.getParent(), zipEntry.name)
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs()
            }
            def os = file.newOutputStream()

            def is = zipFile.getInputStream(zipEntry)
            def len = -1
            def buffer = new byte[1024]
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len)
                os.flush()
            }
            os.close()
            is.close()

            files.put(zipEntry.name, file)
        }
        return files
    }
//    static extractArscFileFromApk(File apkFile) {
//        ZipFile zipFile = new ZipFile(apkFile)
//        ZipEntry arscEntry = zipFile.getEntry("resources.arsc")
//
//        def arscFile = new File(apkFile.getParent(), "resources.arsc")
//        OutputStream os = arscFile.newOutputStream()
//
//        InputStream is = zipFile.getInputStream(arscEntry);
//        int len = -1;
//        byte[] buffer = new byte[1024]
//        while ((len = is.read(buffer)) != -1) {
//            os.write(buffer, 0, len)
//            os.flush()
//        }
//        os.close()
//        is.close()
//        return arscFile
//    }

    static byte[] getArscFileContentFromApk(File apkFile, int offset, int len) {
        ZipFile zipFile = new ZipFile(apkFile)
        ZipEntry arscEntry = zipFile.getEntry("resources.arsc")

        Log.log("getArscFileContentFromApk:", "offset=" + offset + ",len=" + len + ",size=" + arscEntry.size)

        if (offset > arscEntry.size) return null

        if (offset + len > arscEntry.size) len = arscEntry.size - offset

        if (len == 0) return null

        byte[] buffer = new byte[len]
        zipFile.getInputStream(arscEntry).read(buffer, offset, len)

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

//    static extractXmlFilesInApk(File apkFile) {
//        def xmlFiles = [:]
//
//        def zipFile = new ZipFile(apkFile)
//        def entrys = zipFile.entries()
//
//        while (entrys.hasMoreElements()) {
//            def zipEntry = entrys.nextElement()
//            if (zipEntry.name.endsWith(".xml")) {
//
//                def file = new File(apkFile.getParent(), zipEntry.name)
//                if (!file.getParentFile().exists()) {
//                    file.getParentFile().mkdirs()
//                }
//                def os = file.newOutputStream()
//
//                def is = zipFile.getInputStream(zipEntry)
//                def len = -1
//                def buffer = new byte[1024]
//                while ((len = is.read(buffer)) != -1) {
//                    os.write(buffer, 0, len)
//                    os.flush()
//                }
//                os.close()
//                is.close()
//
//                xmlFiles.put(zipEntry.name, file)
//            }
//        }
//        return xmlFiles
//    }

    static replaceFilesInApk(File apkFile, Map<String, File> allFiles) {
        def is = null
        def len = -1
        def buffer = new byte[1024]

        def zos = new ZipOutputStream(new FileOutputStream(apkFile))
        //replace files
        allFiles.each { fileEntry ->
            zos.putNextEntry(new ZipEntry(fileEntry.key))

            is = fileEntry.value.newInputStream()
            while ((len = is.read(buffer)) != -1) {
                zos.write(buffer, 0, len)
                zos.flush()
            }
            is.close()
            zos.closeEntry()
        }

        zos.close()
    }
}
