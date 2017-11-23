package uk.ac.wellcome.platform.reindexer.models

import uk.ac.wellcome.models.Reindex

case class ReindexAttempt(reindex: Reindex,
                          successful: Boolean = false,
                          attempt: Int = 0)
