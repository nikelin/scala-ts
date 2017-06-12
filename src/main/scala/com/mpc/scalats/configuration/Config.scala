package com.mpc.scalats.configuration

import java.io.{File, PrintStream}

/**
  * Created by Milosz on 09.12.2016.
  */
case class Config(
   emitInterfaces: Boolean = true,
   emitClasses: Boolean = false,
   optionToNullable: Boolean = true,
   optionToUndefined: Boolean = false,
   optionToArray: Boolean = false,
   basePackage: String,
   outputFile: File
 )