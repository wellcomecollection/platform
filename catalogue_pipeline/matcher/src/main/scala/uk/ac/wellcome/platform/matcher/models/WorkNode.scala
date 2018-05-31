package uk.ac.wellcome.platform.matcher.models

// Represents an individual node in our graph database.
//
//   - id is the source identifier of the original Work
//   - referencedWorkIds is the list of all the works which this Work
//     has a reference to.  In graph terms, it's the edges starting
//     from this work
//   - componentId is an ID that represents all the works in the same
//     connected component as this work
//
// For example:
//
//      A -----> B <----> C <----- D
//
// In this graph, the WorkNode for B would have:
//
//    - id = B
//    - referencedWorkIds = {C}, because B itself only refers to C
//    - componentId = Id({A, B, C, D}), because these are the four nodes
//      in this connected component.  A, C and D have the same componentId.
//
case class WorkNode(
  id: String,
  referencedWorkIds: List[String],
  componentId: String
)
