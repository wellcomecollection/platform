output "id_minter_topic_name" {
  value = "${module.id_minter_topic.name}"
}

output "id_minter_topic_arn" {
  value = "${module.id_minter_topic.arn}"
}

output "id_minter_topic_publish_policy" {
  value = "${module.id_minter_topic.publish_policy}"
}

output "read_ingestor_config_policy_document" {
  value = "${data.aws_iam_policy_document.s3_read_ingestor_config.json}"
}