package uk.ac.wellcome.models

trait Message {
  val subject: Option[String]
  val body: String
  val topic: String
}
