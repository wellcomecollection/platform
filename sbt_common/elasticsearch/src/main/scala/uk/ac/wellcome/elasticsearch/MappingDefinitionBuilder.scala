package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.mappings.MappingDefinition

trait MappingDefinitionBuilder {
  def buildMappingDefinition(rootIndexType: String): MappingDefinition
}
