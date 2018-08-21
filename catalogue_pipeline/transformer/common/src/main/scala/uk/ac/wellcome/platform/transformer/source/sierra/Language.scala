package uk.ac.wellcome.platform.transformer.source.sierra

// Represents a Language object, as returned by the Sierra API.
// https://techdocs.iii.com/sierraapi/Content/zReference/objects/bibObjectDescription.htm?Highlight=language
case class Language(code: String, name: String)
