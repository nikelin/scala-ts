package com.mpc.scalats.core

import java.io._
import java.net.{URL, URLClassLoader}

import com.mpc.scalats.configuration.Config
import org.clapper.classutil.ClassFinder
import sbt._
import sbt.Keys.fullClasspath

import scala.reflect.runtime.universe._

/**
  * Created by Milosz on 11.06.2016.
  */
object TypeScriptGenerator {

  def generateFromClassNames(classFinder: ClassFinder, fullClasspath: Array[URL])
                            (implicit config: Config) = {
    val classes = classFinder.getClasses
    val matchingClasses = classes
      .filter(v => v.name.contains(config.basePackage))
      .filter(_.isConcrete)
      .filterNot(_.name.endsWith("$"))
      .filterNot(_.name.endsWith("package"))
      .toList

    val cl = new URLClassLoader(fullClasspath, ClassLoader.getSystemClassLoader)

    implicit val mirror = runtimeMirror(cl)
    val types = matchingClasses map { className =>
      mirror.staticClass(className.name.replaceAll("\\$", ".")).toType
    }

    generate(types)(config)
  }

  def generate(caseClasses: List[Type])(implicit config: Config) = {
    val outputStream = new FileOutputStream(config.outputFile)
    val scalaCaseClasses = ScalaParser.parseCaseClasses(caseClasses)
    val typeScriptInterfaces = Compiler.compile(scalaCaseClasses)
    TypeScriptEmitter.emit(typeScriptInterfaces, outputStream)
  }

}
