package uk.ac.wellcome.models

case class Reindex(TableName: String,
                   RequestedVersion: Int,
                   CurrentVersion: Int)
