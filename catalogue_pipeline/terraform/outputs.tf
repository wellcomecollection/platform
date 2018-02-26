output "ecs_services_cluster_name" {
  value = "${module.catalogue_pipeline_cluster.cluster_name}"
}

output "miro_transformer_topic_publish_policy" {
  value = "${module.miro_transformer_topic.publish_policy}"
}

output "vhs_full_access_policy" {
  value = "${module.versioned-hybrid-store.full_access_policy}"
}

output "vhs_dynamodb_update_policy" {
  value = "${module.versioned-hybrid-store.dynamodb_update_policy}"
}

output "vhs_table_name" {
  value = "${module.versioned-hybrid-store.table_name}"
}

output "vhs_table_stream_arn" {
  value = "${module.versioned-hybrid-store.table_stream_arn}"
}

output "vhs_bucket_name" {
  value = "${module.versioned-hybrid-store.bucket_name}"
}

output "vpc_services_id" {
  value = "${module.vpc_services.vpc_id}"
}

output "alb_cloudwatch_id" {
  value = "${module.catalogue_pipeline_cluster.alb_cloudwatch_id}"
}

output "alb_listener_https_arn" {
  value = "${module.catalogue_pipeline_cluster.alb_listener_https_arn}"
}

output "alb_listener_http_arn" {
  value = "${module.catalogue_pipeline_cluster.alb_listener_http_arn}"
}

output "cluster_name" {
  value = "${module.catalogue_pipeline_cluster.cluster_name}"
}
