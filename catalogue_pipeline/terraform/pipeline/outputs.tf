output "rds_access_security_group_id" {
  value = "${aws_security_group.rds_access_security_group.id}"
}

output "cluster_name" {
  value = "${var.namespace}"
}

output "transformer_topic_arn" {
  value = "${module.transformer_topic.arn}"
}

output "es_ingest_topic_arn" {
  value = "${module.es_ingest_topic.arn}"
}

output "transformed_works_topic_arn" {
  value = "${module.transformed_works_topic.arn}"
}

output "recorded_works_topic_arn" {
  value = "${module.recorded_works_topic.arn}"
}

output "merged_works_topic_arn" {
  value = "${module.merged_works_topic.arn}"
}

output "matched_works_topic_arn" {
  value = "${module.matched_works_topic.arn}"
}
