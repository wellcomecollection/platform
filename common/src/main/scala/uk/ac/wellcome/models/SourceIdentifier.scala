package uk.ac.wellcome.models

import com.fasterxml.jackson.module.scala.JsonScalaEnumeration

/** An identifier received from one of the original sources */
case class SourceIdentifier(@JsonScalaEnumeration(classOf[IdentifierSchemeType])identifierScheme: IdentifierSchemes.Value , value: String)
