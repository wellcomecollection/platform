package uk.ac.wellcome.platform.idminter.utils

import java.util.Map.Entry
import scala.collection.JavaConversions._

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.databind.node.{
  ArrayNode, JsonNodeFactory, ObjectNode, TextNode
}

import uk.ac.wellcome.utils.JsonUtil

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
 *
 * In a nutshell, this walks the entire JSON document, and whenever it's
 * on a map/object node, it looks to see if the node is Identifiable.
 * If so, it adds an identifier to the node.  Otherwise the document is
 * passed through unmodified.
 */
object IdentifiableWalker {

  def readTree(jsonString: String): JsonNode = {
    val mapper = new ObjectMapper()
    mapper.readTree(jsonString)
  }

  def identifyDocument(jsonString: String): String = {
    val node = readTree(jsonString)
    JsonUtil.toJson(rebuildObjectNode(node)).get
  }

  private def processValue(value: JsonNode) = {
    if (value.isObject) {
      rebuildObjectNode(value)
    } else if (value.isArray) {
      rebuildArrayNode(value)
    } else {
      value
    }
  }

  private def rebuildObjectNode(node: JsonNode): ObjectNode = {
    val newNode = new ObjectNode(new JsonNodeFactory(false))
    for (field <- node.fields) {
      newNode.set(field.getKey, processValue(field.getValue))
    }

    if (node.has("identifiers") && node.has("ontologyType")) {
      newNode.set("canonicalId", new TextNode("PlaceholderIdentifier"))
    }

    newNode
  }

  private def rebuildArrayNode(node: JsonNode): ArrayNode = {
    val newNode = new ArrayNode(new JsonNodeFactory(false))
    for (elem <- node.elements) {
      newNode.add(processValue(elem))
    }
    newNode
  }
}
