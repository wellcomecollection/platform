package uk.ac.wellcome.platform.idminter.utils

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}

/* This object takes a JSON string (which is assumed to be a map) and walks
 * it, looking for objects that conform to the Identifiable trait.
 * Rather than using first-class Scala types, this does a completely generic
 * tree-walk -- we're trading some type safety for flexibility.
 *
 * In this case, the interesting features of the Identifiable trait are:
 *
 *   - a string field called "ontologyType"
 *   - a list field called "identifiers" which has a non-empty list of
 *     objects which deserialise as instances of SourceIdentifier
 */
object IdentifiableWalker {

  def readTree(jsonString: String): JsonNode = {
    val mapper = new ObjectMapper()
    mapper.readTree(jsonString)
  }

}
