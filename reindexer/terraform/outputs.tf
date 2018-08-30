output "miro_topic_name" {
  value = "${module.miro_reindexer.hybrid_records_topic_name}"
}

output "sierra_topic_name" {
  value = "${module.sierra_reindexer.hybrid_records_topic_name}"
}
