package uk.ac.wellcome.platform.transformer.miro.transformers

trait MiroTransformableUtils {

  /** Some Miro fields contain keys/values in different fields.  For example:
    *
    *     "image_library_ref_department": ["ICV No", "External Reference"],
    *     "image_library_ref_id": ["1234", "Sanskrit manuscript 5678"]
    *
    * This represents the mapping:
    *
    *     "ICV No"             -> "1234"
    *     "External Reference" -> "Sanskrit manuscript 5678"
    *
    * This method takes two such fields, combines them, and returns a list
    * of (key, value) tuples.  Note: we don't use a map because keys aren't
    * guaranteed to be unique.
    */
  def zipMiroFields(
    keys: List[Option[String]],
    values: List[Option[String]]): List[(Option[String], Option[String])] = {
    if (keys.lengthCompare(values.length) != 0) {
      throw new RuntimeException(
        s"Different lengths! keys=$keys, values=$values"
      )
    }

    (keys, values).zipped.toList
  }
}
