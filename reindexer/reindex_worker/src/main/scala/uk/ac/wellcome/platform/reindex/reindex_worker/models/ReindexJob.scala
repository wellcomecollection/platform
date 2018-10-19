package uk.ac.wellcome.platform.reindex.reindex_worker.models

/** A request to identify all the shards in the table that need reindexing. */
case class ReindexJob(segment: Int, totalSegments: Int)
