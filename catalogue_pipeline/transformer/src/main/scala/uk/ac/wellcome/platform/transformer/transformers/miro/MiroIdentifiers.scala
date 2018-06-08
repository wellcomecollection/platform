package uk.ac.wellcome.platform.transformer.transformers.miro

import uk.ac.wellcome.models.work.internal.{IdentifierType, SourceIdentifier}
import uk.ac.wellcome.platform.transformer.source.MiroTransformableData

trait MiroIdentifiers {
  def getIdentifiers(miroData: MiroTransformableData,
                     miroId: String): List[SourceIdentifier] = {
    val miroIDList = List(
      SourceIdentifier(
        identifierType = IdentifierType("miro-image-number"),
        ontologyType = "Work",
        value = miroId)
    )

    // Add the Sierra system number from the INNOPAC ID, if it's present.
    //
    // We add a b-prefix because everything in Miro is a bibliographic record,
    // but there are other types in Sierra (e.g. item, holding) with matching
    // IDs but different prefixes.
    val sierraList: List[SourceIdentifier] = miroData.innopacID match {
      case Some(s) => {

        // The ID in the Miro record is an 8-digit number with a check digit
        // (which may be x).  The format we use for Sierra system numbers
        // is a record type prefix ("b") and 8-digits.
        //
        // Regex explanation:
        //
        //    ^                 start of string
        //    (?:\.?[bB])?      non-capturing group, which trims 'b' or 'B'
        //                      or '.b' or '.B' from the start of the string,
        //                      but *not* a lone '.'
        //    ([0-9]{7}[0-9xX]) capturing group, the 8 digits of the system
        //                      number plus the final check digit, which may
        //                      be an X
        //    $                 end of the string
        //
        val regexMatch = """^(?:\.?[bB])?([0-9]{7}[0-9xX])$""".r.unapplySeq(s)
        regexMatch match {
          case Some(s) =>
            s.map { id =>
              SourceIdentifier(
                identifierType = IdentifierType("sierra-system-number"),
                ontologyType = "Work",
                value = s"b$id")
            }
          case _ =>
            throw new RuntimeException(
              s"Expected 8-digit INNOPAC ID or nothing, got ${miroData.innopacID}"
            )
        }
      }
      case None => List()
    }

    // Add any other legacy identifiers to this record.  Right now we
    // put them all in the same identifier scheme, because we're not doing
    // any transformation or cleaning.
    val libraryRefsList: List[SourceIdentifier] =
    zipMiroFields(
      keys = miroData.libraryRefDepartment,
      values = miroData.libraryRefId)
      .collect {
        case (Some(label), Some(value)) =>
          SourceIdentifier(
            identifierType = IdentifierType("miro-library-reference"),
            ontologyType = "Work",
            value = s"$label $value"
          )
      }

    miroIDList ++ sierraList ++ libraryRefsList
  }
}
