output "reindex_config" {
  value = "${jsonencode("${local.reindexer_jobs}")}"
}
