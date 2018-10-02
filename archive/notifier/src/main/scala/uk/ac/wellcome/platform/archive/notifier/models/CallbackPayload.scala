package uk.ac.wellcome.platform.archive.notifier.models

/** Represents the payload sent to a callback URL.
  *
  * @param id ID of the ingest request that we're sending a callback for.
  */
case class CallbackPayload(id: String)
