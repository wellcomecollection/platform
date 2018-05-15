package uk.ac.wellcome.platform.transformer.source

import io.circe.generic.extras.JsonKey

// Examples of varFields from the Sierra JSON:
//
//    {
//      "fieldTag": "b",
//      "content": "X111658"
//    }
//
//    {
//      "fieldTag": "a",
//      "marcTag": "949",
//      "ind1": "0",
//      "ind2": "0",
//      "subfields": [
//        {
//          "tag": "1",
//          "content": "STAX"
//        },
//        {
//          "tag": "2",
//          "content": "sepam"
//        }
//      ]
//    }
//
case class MarcSubfield(
  tag: String,
  content: String
)

case class VarField(
  fieldTag: String,
  content: Option[String] = None,
  marcTag: Option[String] = None,
  @JsonKey("ind1") indicator1: Option[String] = None,
  @JsonKey("ind2") indicator2: Option[String] = None,
  subfields: List[MarcSubfield] = Nil
)

case object VarField {
  def apply(fieldTag: String, content: String): VarField =
    VarField(
      fieldTag = fieldTag,
      content = Some(content),
      marcTag = None,
      indicator1 = None,
      indicator2 = None,
      subfields = Nil
    )

  def apply(fieldTag: String,
            marcTag: String,
            indicator1: String,
            indicator2: String,
            subfields: List[MarcSubfield]): VarField =
    VarField(
      fieldTag = fieldTag,
      content = None,
      marcTag = Some(marcTag),
      indicator1 = Some(indicator1),
      indicator2 = Some(indicator2),
      subfields = subfields
    )
}

// Examples of fixedFields from the Sierra JSON:
//
//    "98": {
//      "label": "PDATE",
//      "value": "2017-12-22T12:55:57Z"
//    },
//    "77": {
//      "label": "TOT RENEW",
//      "value": 12
//    }
//
case class FixedField(
  label: String,
  value: String
)
