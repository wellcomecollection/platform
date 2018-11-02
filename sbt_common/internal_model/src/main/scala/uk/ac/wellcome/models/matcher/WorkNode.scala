package uk.ac.wellcome.models.matcher

case class WorkNode(id: String,
                    version: Int,
                    linkedIds: List[String],
                    componentId: String)
