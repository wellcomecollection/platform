package uk.ac.wellcome.models

sealed trait AbstractConcept {
  val label: String
  val ontologyType: String
}

// This is an example of a QualifiedConcept and a Concept from Silver's notes:
//
//   "subjects": [
//     {
//       "label": "Tuberculosis - prevention & control.",
//       "type": "QualifiedConcept",
//       "concept": {
//         "id": "hd2891f",
//         "label": "Tuberculosis",
//         "type": "Concept",
//         "identifiers": [
//           {
//             "type": "Identifier",
//             "identifierScheme": "mesh",
//             "value": "D014376Q000517"
//           }
//         ],
//         "qualifiers": [
//           {
//             "qualifierType": "general-subdivision" ,
//             "label": "prevention & control",
//             "type": "Concept"
//           }
//         ]
//       }
//     },
//   ]
//
// In particular, these models assume:
//
//  - A Work can have a mixture of Concept or a QualifiedConcept in the subject
//    and genre fields
//  - The "concept" on a QualifiedConcept is never another QualifiedConcept
//  - The "qualifiers" field can be nested -- so you could have something like:
//
//        "qualifiers": [
//          {
//            "qualifierType": "subdivision",
//            "label": "fizzbuzz",
//            "type": "Concept",
//            "qualifiers": [
//              {
//                "qualifierType": "sub-subdivision",
//                "label": "fizz",
//                "type": "Concept",
//              }
//            ]
//          }
//        ]
//

case class Concept(
  label: String,
  qualifierType: Option[String] = None,
//  qualifiers: List[Concept] = List(),
  ontologyType: String = "Concept"
) extends AbstractConcept

case object Concept {
  def apply(label: String, qualifierType: String): Concept =
    Concept(
      label = label,
      qualifierType = Some(qualifierType)
//      qualifiers = List()
    )

  def apply(label: String,
            qualifierType: String,
            qualifiers: List[Concept]): Concept =
    Concept(
      label = label,
      qualifierType = Some(qualifierType)
//      qualifiers = List()
    )
}

case class QualifiedConcept(
  label: String,
  concept: Concept,
  ontologyType: String = "QualifiedConcept"
) extends AbstractConcept
