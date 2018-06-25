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

                            //change resources.arsc and apk file
                            def newArscFile = modifyArsc(apkFile, packageId)

                            //change AndroidManifest.xml res目录下的所有xml文件
                            Map<String, File> xmlFiles = modifyXml(apkFile, packageId)

                            replaceFileInApk(apkFile, xmlFiles, newArscFile)

                            //change R.java
                            def rFileDir = project.getBuildDir().absolutePath + File.separator + FD_GENERATED +
                                    File.separator + FD_SOURCE + File.separator + FD_R + File.separator + type +
                                    File.separator + Utils.packageName2Path(project.android.defaultConfig.applicationId)
                            def rFile = new File(rFileDir, "R.java")
                            modifyRFile(rFile, project.modify.packageId)
                        }
                    }
                }
            }
        }
    }

    def modifyArsc(File apkFile, int packageId) {
        if (!apkFile.exists() || !apkFile.isFile()) {
            println("######## modifyArsc return " + apkFile.exists() + "," + apkFile.isFile())
            return
        }

        byte[] src = ApkFileUtils.getArscFileContentFromApk(apkFile)
        byte[] newSrc = ArscUtils.changeArscPackageId(src, packageId)
        File newArscFile = ArscUtils.generateNewArscFile(apkFile.getParent(), newSrc)
        return newArscFile
    }

    def modifyXml(File apkFile, int packageId) {
        if (!apkFile.exists() || !apkFile.isFile()) {
            println("######## modifyXml return " + apkFile.exists() + "," + apkFile.isFile())
            return null
        }

        LinkedHashMap<String, File> newXmlFiles = new LinkedHashMap<>()

        def xmlEntrys = ApkFileUtils.getXmlFilesInApk(apkFile)
        xmlEntrys.each {
            byte[] src = ApkFileUtils.getEntryContentFromApk(apkFile, it)
            XmlUtils.changePackageId(src, packageId)
            def newXmlFile = XmlUtils.generateNewXmlFile(apkFile.getParent(), it.name, src)
            newXmlFiles.put(it.name, newXmlFile)
        }
        return newXmlFiles
    }

    def replaceFileInApk(File apkFile, Map<String, File> xmlFiles, File newArscFile) {
        ApkFileUtils.replaceFilesInApk(apkFile, xmlFiles, newArscFile)
        newArscFile.delete()
        xmlFiles.each { xmlFileEntry ->
            xmlFileEntry.value.delete()
        }
    }

    def modifyRFile(File rFile, String packageId) {
        def tmpFile = new File(rFile.parent, 'tmpR.java')
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
