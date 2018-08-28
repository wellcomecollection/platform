package uk.ac.wellcome.platform.reindex.creator.models

/** A request to identify all the shards in the table that need reindexing. */
case class ReindexJob(segment: Int, totalSegments: Int)
