package com.mpc.scalats.core

/**
  * Created by Milosz on 09.06.2016.
  */
import scala.reflect.runtime.universe._

object ScalaParser {

  import ScalaModel._

  def parseCaseClasses(caseClassTypes: List[Type]): List[CaseClass] = {
    caseClassTypes.flatMap(getInvolvedTypes(Set.empty))
      .filter(isCaseClass)
      .distinct.map(parseCaseClass).distinct
  }

  private def parseCaseClass(caseClassType: Type) = {
    val relevantMemberSymbols = caseClassType.members.collect {
      case m: MethodSymbol if m.isCaseAccessor => m
    }
    val typeParams = caseClassType.typeConstructor.normalize match {
      case polyType: PolyTypeApi => polyType.typeParams.map(_.name.decodedName.toString)
      case _ => List.empty[String]
    }
    val members = relevantMemberSymbols map { member =>
      val memberName = member.name.toString
      CaseClassMember(memberName, getTypeRef(member.returnType, typeParams.toSet))
    }

    val caseClassName = caseClassType.typeSymbol.fullName
      .replace(enclosingPackage(caseClassType.typeSymbol).fullName + ".", "")
      .replaceAll("\\.", "")

    CaseClass(
      caseClassName,
      members.toList,
      typeParams
    )
  }

  private def getInvolvedTypes(alreadyExamined: Set[Type])(scalaType: Type): List[Type] = {
    try {
      if (!alreadyExamined.contains(scalaType) && !scalaType.typeSymbol.isParameter) {
        val relevantMemberSymbols = scalaType.members.collect {
          case m: MethodSymbol if m.isCaseAccessor => m
        }
        val memberTypes = relevantMemberSymbols.map(_.typeSignature match {
          case NullaryMethodType(resultType) => resultType
          case t => t
        }).flatMap(getInvolvedTypes(alreadyExamined + scalaType))
        val typeArgs = scalaType match {
          case t: scala.reflect.runtime.universe.TypeRef => t.args.flatMap(getInvolvedTypes(alreadyExamined + scalaType))
          case _ => List.empty
        }
        (scalaType.typeConstructor :: typeArgs ::: memberTypes.toList).filter(!_.typeSymbol.isParameter).distinct
      } else {
        List.empty
      }
    } catch {
      case e: Throwable =>
        println(s"Failed to parse ${scalaType.termSymbol.toString} ${scalaType.typeSymbol.toString}")
        e.printStackTrace()
        throw e
    }
  }

  private def getTypeRef(scalaType: Type, typeParams: Set[String]): TypeRef = {
    val typeName = scalaType.typeSymbol.name.toString
    typeName match {
      case "Int" | "Byte" =>
        IntRef
      case "Long" =>
        LongRef
      case "Double" =>
        DoubleRef
      case "Boolean" =>
        BooleanRef
      case "String" =>
        StringRef
      case "List" | "Seq" | "Set" =>
        val innerType = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.head
        SeqRef(getTypeRef(innerType, typeParams))
      case "Option" =>
        val innerType = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.head
        OptionRef(getTypeRef(innerType, typeParams))
      case "LocalDate"  =>
        DateRef
      case "Instant" | "Timestamp" =>
        DateTimeRef
      case typeParam if typeParams.contains(typeParam) =>
        TypeParamRef(typeParam)
      case _ if isCaseClass(scalaType) =>
        val caseClassName = scalaType.typeSymbol.fullName
          .replace(enclosingPackage(scalaType.typeSymbol).fullName + ".", "")
          .replaceAll("\\.", "")

        val typeArgs = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args
        val typeArgRefs = typeArgs.map(getTypeRef(_, typeParams))
        CaseClassRef(caseClassName, typeArgRefs)
      case _ =>
        UnknownTypeRef(typeName)
    }
  }

  private def enclosingPackage(sym: Symbol): Symbol = {
    if (sym == NoSymbol) NoSymbol
    else if (sym.isPackage) sym
    else enclosingPackage(sym.owner)
  }

  private def isCaseClass(scalaType: Type) =
    scalaType.members.collect({ case m: MethodSymbol if m.isCaseAccessor => m }).nonEmpty

}