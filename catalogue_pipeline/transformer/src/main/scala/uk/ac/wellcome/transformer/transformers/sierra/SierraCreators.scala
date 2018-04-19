// @@AWLC These are commented out as creators are being replaced by
// contributors.  I'll replace these with a proper contributors
// implementation in a separate pull request.
//

// package uk.ac.wellcome.transformer.transformers.sierra
//
// import uk.ac.wellcome.models._
// import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData}
//
// trait SierraCreators extends MarcUtils {
//
//   /* Populate wwork:creators. Rules:
//    *
//    * . For all bibliographic records use "marcTag": "100", "110"
//    * . Platform "label" populate from subfield "a"
//    * . For "100" type as "Person" populate "prefix" from 100 subfield c and "numeration" from b
//    * . For "110" type as "Organisation"
//    * . If subfield 0 contains a value use it to populate "identifiers". The "identifierScheme" will be lc-names.
//    */
//   def getCreators(
//     bibData: SierraBibData): List[MaybeDisplayable[AbstractAgent]] = {
//
//     val persons =
//       getMatchingSubfields(bibData, "100", List("a", "b", "c", "0")).map {
//         subfields =>
//           val name = subfields.collectFirst {
//             case MarcSubfield("a", content) => content
//           }
//           val numeration = subfields.collectFirst {
//             case MarcSubfield("b", content) => content
//           }
//           val prefixes = subfields.collect {
//             case MarcSubfield("c", content) => content
//           }
//           val codes = getIdentifierCodes(subfields)
//           val prefixString =
//             if (prefixes.isEmpty) None else Some(prefixes.mkString(" "))
//
//           val person = Person(
//             label = name.get,
//             prefix = prefixString,
//             numeration = numeration)
//
//           identify(codes, person, "Person")
//       }
//
//     val organisations =
//       getMatchingSubfields(bibData, "110", List("a", "0")).map { subfields =>
//         val name = subfields.collectFirst {
//           case MarcSubfield("a", content) => content
//         }
//         val codes = getIdentifierCodes(subfields)
//         val organisation = Organisation(name.get)
//         identify(codes, organisation, "Organisation")
//       }
//
//     persons ++ organisations
//   }
//
//   /**
//     * Identifier codes contain inconsistent spacing as in " nr 82270463" vs "nr 82270463"
//     * vs " nr82270463" and so on. We remove all spaces so that all of the above reduce to "nr82270463"
//     */
//   private def getIdentifierCodes(subfields: List[MarcSubfield]) = {
//     subfields.collect {
//       case MarcSubfield("0", content) => content.replaceAll("\\s", "")
//     }
//   }
//
//   private def identify(codes: List[String],
//                        agent: AbstractAgent,
//                        ontologyType: String) = {
//     codes.distinct match {
//       case Nil => Unidentifiable(agent)
//       case Seq(code) =>
//         val sourceIdentifier = SourceIdentifier(
//           identifierScheme = IdentifierSchemes.libraryOfCongressNames,
//           ontologyType = ontologyType,
//           value = code)
//         Identifiable(agent, sourceIdentifier, List(sourceIdentifier))
//       case _ =>
//         throw new RuntimeException(
//           s"Multiple identifiers in subfield $$0: $codes")
//     }
//   }
// }
