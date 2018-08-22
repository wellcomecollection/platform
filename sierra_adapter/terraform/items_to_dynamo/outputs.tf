output "topic_name" {
  value = "${module.sierra_to_dynamo_updates_topic.name}"
}

output "vhs_bucket_name" {
  value = "${local.vhs_sierra_items_bucket_name}"
}
