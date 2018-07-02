package com.lzp.modifyplugin

import org.gradle.api.Plugin
import org.gradle.api.Project

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
            project.android.applicationVariants.all { variant ->
                def variantName = variant.name.capitalize()
//                Log.log('variantName', variantName)
                def task = project.tasks["process${variantName}Resources"]
                task.doLast {
                    if (project.modify.packageId == null) return

                    File resPackageOutputFolder
                    File resOutBaseNameFile
                    File sourceOutputDir = task.sourceOutputDir
                    File textSymbolOutputFile

                    if (task.hasProperty('resPackageOutputFolder')) {
                        //com.android.tools.build:gradle:3.+
                        resPackageOutputFolder = task.resPackageOutputFolder
                        resOutBaseNameFile = new File(resPackageOutputFolder, "resources-${variant.name}.ap_")
                        textSymbolOutputFile = task.getTextSymbolOutputFile()
                    } else if (task.hasProperty('packageOutputFile')) {
                        //com.android.tools.build:gradle:2.+
                        resPackageOutputFolder = task.packageOutputFile.getParentFile()
                        resOutBaseNameFile = task.packageOutputFile
                        textSymbolOutputFile = new File(task.textSymbolOutputDir,'R.txt')
                    }

                    Log.log('resPackageOutputFolder', resPackageOutputFolder.absolutePath)
                    Log.log('sourceOutputDir', sourceOutputDir.absolutePath)

                    def packageId = Integer.decode(project.modify.packageId)
                    Log.log("package id", project.modify.packageId)
                    //0x00~0x7f之间 0x00(SharedLibrary)、0x01(System)、0x7f(App/AppFeature)
                    if (packageId > 1 && packageId < 127) {
                        //extract all files from apk
                        def allFiles = ApkFileUtils.extractFilesFromApk(resOutBaseNameFile)

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

                        replaceFileInApk(resOutBaseNameFile, allFiles)

                        //change R.java
                        modifyRFile(sourceOutputDir, project.modify.packageId)

                        //chnge R.txt
                        modifyRTxt(textSymbolOutputFile, project.modify.packageId)
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
        File resDir = new File(apkFile.getParentFile(), 'res')
        if (resDir.exists() && resDir.isDirectory()) {
            resDir.deleteDir()
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

    private modifyRTxt(File textSymbolOutputFile, String packageId) {
        def tmpFile = new File(textSymbolOutputFile.getParentFile(), 'R.txt_')
        def writer = new BufferedWriter(new FileWriter(tmpFile))

        textSymbolOutputFile.eachLine { line ->
            def pos = -1
            if ((pos = line.indexOf('0x7f')) != -1) {
                def str = line.replace('0x7f', packageId)
                writer.writeLine(str)
            } else {
                writer.writeLine(line)
            }
        }
        writer.close()

        textSymbolOutputFile.delete()
        tmpFile.renameTo(textSymbolOutputFile)
    }
}
