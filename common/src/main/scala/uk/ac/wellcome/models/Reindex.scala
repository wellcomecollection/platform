package uk.ac.wellcome.models

case class Reindex(TableName: String,
                   ReindexShard: String,
                   RequestedVersion: Int,
                   CurrentVersion: Int)
