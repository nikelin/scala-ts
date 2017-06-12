package com.mpc.scalats.sbt

import java.io.PrintStream
import java.net.URLClassLoader

import com.mpc.scalats.configuration.Config
import com.mpc.scalats.core.TypeScriptGenerator
import sbt.Keys._
import sbt._
import complete.DefaultParsers._
import org.clapper.classutil.ClassFinder

object TypeScriptGeneratorPlugin extends AutoPlugin {

  object autoImport {
    val generateTypeScript = inputKey[Unit]("Generate Type Script")

    val basePackage = settingKey[String]("Package to scan for case-classes to generate definitions from")
    val emitInterfaces = settingKey[Boolean]("Generate interface declarations")
    val emitClasses = settingKey[Boolean]("Generate class declarations")
    val optionToNullable = settingKey[Boolean]("Option types will be compiled to 'type | null'")
    val optionToUndefined = settingKey[Boolean]("Option types will be compiled to 'type | undefined'")
    val optionToArray = settingKey[Boolean]("Option types will be compiled to 'Array<type>'")
    val outputFile  = settingKey[File]("File to where generated output will be written")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    generateTypeScript := {
      implicit val config = Config(
        (emitInterfaces in generateTypeScript).value,
        (emitClasses in generateTypeScript).value,
        (optionToNullable in generateTypeScript).value,
        (optionToUndefined in generateTypeScript).value,
        (optionToArray in generateTypeScript).value,
        (basePackage in generateTypeScript).value,
        (outputFile in generateTypeScript).value
      )

      val classPath = (fullClasspath in Runtime).value.files
      TypeScriptGenerator.generateFromClassNames(ClassFinder(classPath), classPath.map(_.asURL).toArray)
    },
    emitInterfaces in generateTypeScript := true,
    emitClasses in generateTypeScript := false,
    optionToNullable in generateTypeScript := false,
    optionToUndefined in generateTypeScript := false,
    optionToArray in generateTypeScript := true
  )

}
