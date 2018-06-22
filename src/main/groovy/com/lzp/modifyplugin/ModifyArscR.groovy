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

                            //change resources.arsc and apk file
                            def apkFileDir = project.getBuildDir().absolutePath + File.separator + FD_INTERMEDIATES + File.separator + FD_RES + File.separator + type
                            println("######## apkFileDir=" + apkFileDir)
                            def apkFile = new File(apkFileDir, "resources-" + type + ".ap_ ")
                            modifyArsc(apkFile, packageId)

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
        if (!apkFile.exists() || !apkFile.isFile())
            return

        byte[] src = ArscFileUtils.getArscFileContentFromApk(apkFile)
        byte[] newSrc = ArscFileUtils.changeArscPackageId(src, packageId)
        File newArscFile = ArscFileUtils.generateNewArscFile(apkFile.getParent(), newSrc)
        ArscFileUtils.replaceArscFile(apkFile, newArscFile)
        newArscFile.delete()
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
