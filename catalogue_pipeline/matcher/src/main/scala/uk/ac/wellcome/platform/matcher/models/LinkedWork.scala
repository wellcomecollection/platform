package uk.ac.wellcome.platform.matcher.models

case class LinkedWork(workId: String,
                      linkedIds: List[String],
                      setId: String)
