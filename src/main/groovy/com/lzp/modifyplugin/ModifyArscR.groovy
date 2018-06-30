package com.lzp.modifyplugin

import org.gradle.api.Plugin
import org.gradle.api.Project

import java.lang.reflect.Field

class ModifyArscR implements Plugin<Project> {

    private static final String FD_RES = "res"
    private static final String FD_SOURCE = "source"
    private static final String FD_R = "r"

    private static final String FD_INTERMEDIATES = "intermediates";
    private static final String FD_GENERATED = "generated";

    private Project project

    @Override
    public void apply(Project project) {
        this.project = project

        project.extensions.create('modify', ModifyExtension.class)

        project.afterEvaluate {
            project.tasks.all { task ->
                if (task.name.contains("process") && task.name.contains("Resources")) {
                    task.doLast {
                        if (project.modify.packageId == null) return

                        def packageId = Integer.decode(project.modify.packageId)
                        println("######## new package id=" + project.modify.packageId)

                        //0x00~0x7f之间 0x00(SharedLibrary)、0x01(System)、0x7f(App/AppFeature)
                        if (packageId > 1 && packageId < 127) {
                            def start = task.name.indexOf("process") + "process".length()
                            def end = task.name.indexOf("Resources")
                            def type = task.name.substring(start, end).toLowerCase()

                            def apkFileDir = project.getBuildDir().absolutePath + File.separator + FD_INTERMEDIATES + File.separator + FD_RES + File.separator + type
//                            def apkFileDir = project.getBuildDir().absolutePath + File.separator + FD_INTERMEDIATES + File.separator + FD_RES
                            def apkFile = new File(apkFileDir, "resources-" + type + ".ap_ ")
                            println("######## apkFile=" + apkFile.getAbsolutePath())

                            //extract all files from apk
                            def allFiles = ApkFileUtils.extractFilesFromApk(apkFile)
//                            allFiles.each { fileEntry ->
//                                Log.log(fileEntry.key, fileEntry.value.absolutePath)
//                            }

                            //change resources.arsc and apk file
                            def arscFile = allFiles.get('resources.arsc')
                            modifyArsc(arscFile, packageId)

                            //change AndroidManifest.xml res目录下的所有xml文件
                            def xmlFiles = [:]
                            allFiles.each { entry ->
                                entry.key.endsWith('.xml')
                                xmlFiles.put(entry.key, entry.value)
                            }
                            modifyXml(xmlFiles, packageId)

                            replaceFileInApk(apkFile, allFiles)

                            //change R.java
                            def rFileDir = new File(project.getBuildDir().absolutePath + File.separator + FD_GENERATED +
                                    File.separator + FD_SOURCE + File.separator + FD_R + File.separator + type)
                            modifyRFile(rFileDir, project.modify.packageId)
                        }
                    }
                }
            }
        }
    }

    def modifyArsc(File arscFile, int packageId) {
//        Log.log("modifyArsc", arscFile.absolutePath)
//        ArscUtils.changeArscPackageId(arscFile, packageId)
        ArscFile arsc = new ArscFile(arscFile)
        arsc.modifyArsc(packageId)
        arsc.close()
    }

    def modifyXml(Map<String, File> xmlFiles, int packageId) {
        xmlFiles.each { entry ->
            XmlUtils.changePackageId(entry.value, packageId)
        }
    }

    def replaceFileInApk(File apkFile, Map<String, File> allFiles) {
        ApkFileUtils.replaceFilesInApk(apkFile, allFiles)

        allFiles.each { fileEntry ->
            fileEntry.value.delete()
        }
    }

    def modifyRFile(File rDir, String packageId) {
        rDir.eachFile { file ->
//            Log.log('modifyRFile', file.absolutePath)
            if (file.isFile() && file.name.equals("R.java")) {
                realModifyRFile(file, packageId)
            } else if (file.isDirectory()) {
                modifyRFile(file, packageId)
            }
        }
    }

    private realModifyRFile(File rFile, String packageId) {
        def tmpFile = new File(rFile.parent, 'R.ja_')
        def writer = new BufferedWriter(new FileWriter(tmpFile))

        rFile.eachLine { line ->
            def pos = -1
            if ((pos = line.indexOf('0x7f')) != -1) {
                def str = line.replace('0x7f', packageId)
                writer.writeLine(str)
            } else {
                writer.writeLine(line)
            }
        }
        writer.close()

        rFile.delete()
        tmpFile.renameTo(rFile)
    }
}
