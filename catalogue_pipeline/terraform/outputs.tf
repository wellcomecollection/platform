output "dynamodb_table_miro_table_name" {
  value = "${aws_dynamodb_table.miro_table.name}"
}

output "dynamodb_table_reindex_tracker_stream_arn" {
  value = "${aws_dynamodb_table.reindex_tracker.stream_arn}"
}

output "ecs_services_cluster_name" {
  value = "${aws_ecs_cluster.services.name}"
}

output "miro_transformer_topic_publish_policy" {
  value = "${module.miro_transformer_topic.publish_policy}"
}

output "ecs_services_cluster_id" {
  value = "${aws_ecs_cluster.services.id}"
}

output "table_miro_data_arn" {
  value = "${aws_dynamodb_table.miro_table.arn}"
}

output "table_miro_data_name" {
  value = "${aws_dynamodb_table.miro_table.name}"
}

output "vhs_full_access_policy" {
  value = "${module.versioned-hybrid-store.full_access_policy}"
}

output "vhs_table_name" {
  value = "${module.versioned-hybrid-store.table_name}"
}

output "vhs_bucket_name" {
  value = "${module.versioned-hybrid-store.bucket_name}"
}
