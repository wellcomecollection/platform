output "merger_repository_url" {
  value = "${module.ecr_repository_merger.repository_url}"
}

output "topic_arn" {
  value = "${module.sierra_to_dynamo_updates_topic.arn}"
}
