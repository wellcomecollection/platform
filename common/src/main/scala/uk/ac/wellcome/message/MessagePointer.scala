package uk.ac.wellcome.message

import java.net.URI


case class MessagePointer(src: URI)

object MessagePointer {
  def apply(uri: String): MessagePointer = MessagePointer(new URI(uri))
}
