package uk.ac.wellcome.platform.reindexer.models

import uk.ac.wellcome.models.{Reindex, Reindexable}

case class ReindexAttempt(reindex: Reindex,
                          successful: List[Reindexable[String]] = Nil,
                          attempt: Int = 0)