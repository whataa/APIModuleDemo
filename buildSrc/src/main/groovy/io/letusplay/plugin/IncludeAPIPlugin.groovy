package io.letusplay.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

class IncludeAPIPlugin implements Plugin<Project> {

  List<String> apiSdks = new ArrayList<>()
  List<String> copyFiles = new ArrayList<>()

  @Override
  void apply(Project project) {

    Configuration configurations = project.configurations.create("includeApi")
    configurations.canBeResolved = true
    configurations.canBeConsumed = true
    configurations.visible = true

    project.afterEvaluate {
      // 两种解决 compile-task无法编译*.*api文件 方案：
      // 1. 直接动态依赖module自己暴露的 sdk-api
//      project.parent.allprojects { p ->
//        if (p.name == "${project.name}-api") {
//          project.dependencies.add("api", p)
//        }
//      }

      // 2. 在编译前将module里的api文件还原为java/kt，编译后删除
      if (project.plugins.hasPlugin("com.android.application")) {
        copyedFiles.clear()
        project.parent.allprojects { Project p ->
          p.fileTree(p.projectDir).include("**/*api")?.each { File file ->
            println("find .api in ${p.name}: ${file}")
            if (file.name.endsWith(".japi")) {
              def dest = new File(file.absolutePath.replace(".japi", ".java"))
              if (dest.exists()) dest.delete()
              dest << file.text
              copyFiles.add(dest)
            } else if (file.name.endsWith(".kapi")) {
              def dest = new File(file.absolutePath.replace(".kapi", ".kt"))
              if (dest.exists()) dest.delete()
              dest << file.text
              copyFiles.add(dest)
            }
          }
        }
        project.parent.gradle.buildFinished {
          println("buildFinished: ${project.name}")
          copyFiles.forEach { File file ->
            println("delete tmp file: ${file}")
            file.delete()
          }
        }
      }

      configurations.allDependencies.all {
        dep ->
          String targetName = "${dep.name}-api"
          if (!apiSdks.contains(targetName)) {
            project.dependencies.add("api", project.getRootProject().project(targetName))
            apiSdks.add(targetName)
          }
      }
    }
  }
}
