output "dynamodb_table_miro_table_name" {
  value = "${aws_dynamodb_table.miro_table.name}"
}

output "ecs_services_cluster_name" {
  value = "${module.catalogue_pipeline_cluster.cluster_name}"
}

output "miro_transformer_topic_publish_policy" {
  value = "${module.miro_transformer_topic.publish_policy}"
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
