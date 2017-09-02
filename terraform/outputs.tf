output "ecr_nginx" {
  value = "${module.ecr_repository_nginx.repository_url}"
}

output "terraform_apply_topic" {
  value = "${module.terminal_failure_alarm.arn}"
}

output "mets_ingest_arn" {
  value = "${aws_s3_bucket.mets-ingest.arn}"
}

output "wellcomecollection_mets_ingest_arn" {
  value = "${aws_s3_bucket.wellcomecollection-mets-ingest.arn}"
}

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

output "dynamodb_table_deployments_name" {
  value = "${aws_dynamodb_table.deployments.name}"
}

output "lambda_error_alarm_arn" {
  value = "${module.lambda_error_alarm.arn}"
}

output "bucket_infra_arn" {
  value = "${aws_s3_bucket.infra.arn}"
}

output "bucket_infra_id" {
  value = "${aws_s3_bucket.infra.id}"
}

output "bucket_dashboard_id" {
  value = "${aws_s3_bucket.dashboard.id}"
}

output "ecs_container_instance_state_change_arn" {
  value = "${aws_cloudwatch_event_rule.ecs_container_instance_state_change.arn}"
}

output "ecs_container_instance_state_change_name" {
  value = "${aws_cloudwatch_event_rule.ecs_container_instance_state_change.name}"
}

output "load_test_results_arn" {
  value = "${module.load_test_results.arn}"
}

output "service_scheduler_topic_arn" {
  value = "${module.service_scheduler_topic.arn}"
}

output "dynamo_capacity_topic_arn" {
  value = "${module.dynamo_capacity_topic.arn}"
}

output "ec2_terminating_topic_arn" {
  value = "${module.ec2_terminating_topic.arn}"
}

output "ecs_services_cluster_name" {
  value = "${aws_ecs_cluster.services.name}"
}

output "dlq_alarm_arn" {
  value = "${module.dlq_alarm.arn}"
}
output "ec2_instance_terminating_for_too_long_alarm_arn" {
  value = "${module.ec2_instance_terminating_for_too_long_alarm.arn}"
}

output "alb_server_error_alarm_arn" {
  value = "${module.alb_server_error_alarm.arn}"
}

output "terminal_failure_alarm_arn" {
  value = "${module.terminal_failure_alarm.arn}"
}

output "old_deployments_arn" {
  value = "${module.old_deployments.arn}"
}
