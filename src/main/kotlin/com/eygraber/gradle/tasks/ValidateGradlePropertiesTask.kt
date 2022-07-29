package com.eygraber.gradle.tasks

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.PathSensitivity

public fun Project.registerValidateGradlePropertiesTask(
  includedBuildName: String = "buildLogic"
) {
  tasks.register("validateGradleProperties") { task ->
    val wrapperProperties = arrayOf(
      rootProject.file("gradle/wrapper/gradle-wrapper.properties"),
      rootProject.file("$includedBuildName/gradle/wrapper/gradle-wrapper.properties")
    )

    val properties = arrayOf(
      rootProject.file("gradle.properties"),
      rootProject.file("$includedBuildName/gradle.properties")
    )

    task.inputs.files(wrapperProperties).withPathSensitivity(PathSensitivity.RELATIVE)
    task.inputs.files(properties).withPathSensitivity(PathSensitivity.RELATIVE)

    task.outputs.file(
      layout.buildDirectory.file("reports/gradle-properties-match")
    )

    task.doFirst {
      val wrapperPropertiesMatch = wrapperProperties.mapTo(HashSet(wrapperProperties.size)) { it.readText() }.size == 1
      val propertiesMatch = properties.mapTo(HashSet(properties.size)) { it.readText() }.size == 1
      if(wrapperPropertiesMatch && propertiesMatch) {
        task.outputs.files.first().writeText("1")
      }
      else {
        task.outputs.files.first().delete()
        throw GradleException(
          """
          |Gradle properties don't match
          |  wrapperPropertiesMatch = $wrapperPropertiesMatch
          |  propertiesMatch = $propertiesMatch
          """.trimMargin()
        )
      }
    }
  }
}
