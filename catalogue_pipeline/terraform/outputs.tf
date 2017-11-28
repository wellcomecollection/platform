output "miro_table_stream_arn" {
  value = "${aws_dynamodb_table.miro_table.stream_arn}"
}

output "miro_transformer_topic_arn" {
  value = "${module.miro_transformer_topic.arn}"
}

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

output "calm_transformer_topic_publish_policy" {
  value = "${module.calm_transformer_topic.publish_policy}"
}

output "aws_ecs_cluster_services_id" {
  value = "${aws_ecs_cluster.services.id}"
}

output "table_miro_data_arn" {
  value = "${aws_dynamodb_table.miro_table.arn}"
}

output "table_miro_data_name" {
  value = "${aws_dynamodb_table.miro_table.name}"
}

output "vpc_services_id" {
  value = "${module.vpc_services.vpc_id}"
}

output "services_alb_listener_http_arn" {
  value = "${module.services_alb.listener_http_arn}"
}

output "services_alb_listener_https_arn" {
  value = "${module.services_alb.listener_https_arn}"
}

output "services_alb_cloudwatch_id" {
  value = "${module.services_alb.cloudwatch_id}"
}
