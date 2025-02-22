package com.eygraber.conventions.kotlin

import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

public fun Project.configureKgp(
  jdkVersion: Provider<String>,
  jvmDistribution: JvmVendorSpec? = null,
  allWarningsAsErrors: Boolean = true,
  explicitApiMode: ExplicitApiMode = ExplicitApiMode.Disabled,
  configureJavaCompatibility: Boolean = true,
  useK2: Boolean = false,
  freeCompilerArgs: List<KotlinFreeCompilerArg> = emptyList(),
  vararg optIns: KotlinOptIn
) {
  configureKgp(
    jdkVersion.get(),
    jvmDistribution,
    allWarningsAsErrors,
    explicitApiMode,
    configureJavaCompatibility,
    useK2,
    freeCompilerArgs,
    *optIns
  )
}

public fun Project.configureKgp(
  jdkVersion: String,
  jvmDistribution: JvmVendorSpec? = null,
  allWarningsAsErrors: Boolean = true,
  explicitApiMode: ExplicitApiMode = ExplicitApiMode.Disabled,
  configureJavaCompatibility: Boolean = true,
  useK2: Boolean = false,
  freeCompilerArgs: List<KotlinFreeCompilerArg> = emptyList(),
  vararg optIns: KotlinOptIn
) {
  if(configureJavaCompatibility) {
    tasks.withType(JavaCompile::class.java) {
      sourceCompatibility = jdkVersion
      targetCompatibility = jdkVersion
    }
  }

  plugins.withType(KotlinBasePluginWrapper::class.java) {
    val isKmp = this is KotlinMultiplatformPluginWrapper
    with(extensions.getByType(KotlinProjectExtension::class.java)) {
      when(explicitApiMode) {
        ExplicitApiMode.Strict -> explicitApi()
        ExplicitApiMode.Warning -> explicitApiWarning()
        ExplicitApiMode.Disabled -> explicitApi = null
      }

      plugins.withType(JavaBasePlugin::class.java) {
        jvmToolchain {
          languageVersion.set(JavaLanguageVersion.of(jdkVersion.removePrefix("1.")))
          if(jvmDistribution != null) {
            vendor.set(jvmDistribution)
          }
        }
      }

      if(isKmp) {
        sourceSets.configureEach {
          for(optIn in optIns) {
            languageSettings.optIn(optIn.value)
          }
        }
      }
    }

    tasks.withType(KotlinCompilationTask::class.java).configureEach {
      compilerOptions.allWarningsAsErrors.set(allWarningsAsErrors)
      if(this is KotlinJvmCompile) {
        compilerOptions.jvmTarget.set(JvmTarget.fromTarget(jdkVersion))
        if(useK2) {
          compilerOptions.languageVersion.set(KotlinVersion.KOTLIN_2_0)
        }
      }
      compilerOptions.freeCompilerArgs.addAll(
        freeCompilerArgs.map { freeCompilerArg -> freeCompilerArg.value }
      )
      if(!isKmp) {
        compilerOptions.freeCompilerArgs.addAll(
          optIns.map { optIn -> "-opt-in=${optIn.value}" }
        )
      }
    }
  }
}

public val Project.kotlinMultiplatform: KotlinMultiplatformExtension
  get() = extensions.getByType(KotlinMultiplatformExtension::class.java)
