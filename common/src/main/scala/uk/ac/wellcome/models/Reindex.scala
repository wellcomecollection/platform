package uk.ac.wellcome.models

case class Reindex(TableName: String,
                   requestedVersion: Int,
                   currentVersion: Int)