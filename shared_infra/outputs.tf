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

output "ecs_container_instance_state_change_arn" {
  value = "${aws_cloudwatch_event_rule.ecs_container_instance_state_change.arn}"
}

output "ecs_container_instance_state_change_name" {
  value = "${aws_cloudwatch_event_rule.ecs_container_instance_state_change.name}"
}

output "load_test_results_arn" {
  value = "${module.load_test_results.arn}"
}

output "ec2_terminating_topic_arn" {
  value = "${module.ec2_terminating_topic.arn}"
}

output "ec2_instance_terminating_for_too_long_alarm_arn" {
  value = "${module.ec2_instance_terminating_for_too_long_alarm.arn}"
}

output "ecs_services_cluster_name" {
  value = "${aws_ecs_cluster.services.name}"
}

output "dlq_alarm_arn" {
  value = "${module.dlq_alarm.arn}"
}

output "alb_server_error_alarm_arn" {
  value = "${module.alb_server_error_alarm.arn}"
}

output "alb_client_error_alarm_arn" {
  value = "${module.alb_client_error_alarm.arn}"
}

output "terminal_failure_alarm_arn" {
  value = "${module.terminal_failure_alarm.arn}"
}

output "ec2_terminating_topic_publish_policy" {
  value = "${module.ec2_terminating_topic.publish_policy}"
}

output "miro_transformer_topic_publish_policy" {
  value = "${module.miro_transformer_topic.publish_policy}"
}

output "calm_transformer_topic_publish_policy" {
  value = "${module.calm_transformer_topic.publish_policy}"
}

output "bucket_infra_arn" {
  value = "${aws_s3_bucket.infra.arn}"
}

output "bucket_infra_id" {
  value = "${aws_s3_bucket.infra.id}"
}

output "bucket_dashboard_arn" {
  value = "${aws_s3_bucket.dashboard.arn}"
}

output "bucket_dashboard_id" {
  value = "${aws_s3_bucket.dashboard.id}"
}

output "bucket_miro_data_id" {
  value = "${aws_s3_bucket.miro-data.id}"
}

output "bucket_miro_data_arn" {
  value = "${aws_s3_bucket.miro-data.arn}"
}

output "bucket_miro_images_sync_arn" {
  value = "${aws_s3_bucket.miro-images-sync.arn}"
}

output "bucket_miro_images_sync_id" {
  value = "${aws_s3_bucket.miro-images-sync.id}"
}

# This should be removed when they are superseded by bucket_wellcomecollectio_miro_images_public_* below
output "bucket_miro_images_public_arn" {
  value = "${aws_s3_bucket.miro_images_public.arn}"
}

output "bucket_miro_images_public_id" {
  value = "${aws_s3_bucket.miro_images_public.id}"
}

output "bucket_wellcomecollectio_miro_images_public_arn" {
  value = "${aws_s3_bucket.wellcomecollection-miro-images-public.arn}"
}

output "bucket_wellcomecollectio_miro_images_public_id" {
  value = "${aws_s3_bucket.wellcomecollection-miro-images-public.id}"
}

output "table_miro_data_arn" {
  value = "${aws_dynamodb_table.miro_table.arn}"
}

output "table_miro_data_name" {
  value = "${aws_dynamodb_table.miro_table.name}"
}

# Outputs required for Loris

output "cloudfront_logs_domain_name" {
  value = "${aws_s3_bucket.cloudfront-logs.bucket_domain_name}"
}

output "vpc_api_id" {
  value = "${module.vpc_api.vpc_id}"
}

output "vpc_api_subnets" {
  value = "${module.vpc_api.subnets}"
}

output "api_cluster_asg_instance_sg_id" {
  value = "${module.api_cluster_asg.instance_sg_id}"
}

output "ecs_cluster_api_id" {
  value = "${aws_ecs_cluster.api.id}"
}

output "api_alb_listener_https_arn" {
  value = "${module.api_alb.listener_https_arn}"
}

output "api_alb_listener_http_arn" {
  value = "${module.api_alb.listener_http_arn}"
}

output "api_alb_cloudwatch_id" {
  value = "${module.api_alb.cloudwatch_id}"
}

output "api_userdata_efs_mount_directory" {
  value = "${module.api_userdata.efs_mount_directory}"
}
