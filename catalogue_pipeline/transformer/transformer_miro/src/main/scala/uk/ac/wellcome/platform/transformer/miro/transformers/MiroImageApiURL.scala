package uk.ac.wellcome.platform.transformer.miro.transformers

trait MiroImageApiURL {
  def buildImageApiURL(miroId: String, templateName: String): String = {
    val iiifImageApiBaseUri = "https://iiif.wellcomecollection.org"

    val imageUriTemplates = Map(
      "thumbnail" -> "%s/image/%s.jpg/full/300,/0/default.jpg",
      "info" -> "%s/image/%s.jpg/info.json"
    )

    val imageUriTemplate = imageUriTemplates.getOrElse(
      templateName,
      throw new Exception(
        s"Unrecognised Image API URI template ($templateName)!"))

    imageUriTemplate.format(iiifImageApiBaseUri, miroId)
  }
}
