# Role policies for the Elasticsearch ingestor

resource "aws_iam_role_policy" "ecs_ingestor_task_read_ingestor_q" {
  name   = "ecs_task_ingestor_policy"
  role   = "${module.ecs_ingestor_iam.task_role_name}"
  policy = "${module.es_ingest_queue.read_policy}"
}

resource "aws_iam_role_policy" "ecs_ingestor_task_cloudwatch_metric" {
  name   = "ecs_task_cloudwatch_metric_policy"
  role   = "${module.ecs_ingestor_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

# Role policies for the Calm adapter

resource "aws_iam_role_policy" "ecs_calm_adapter_task" {
  name   = "ecs_task_calm_adapter_policy"
  role   = "${module.ecs_calm_adapter_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_calm_db_all.json}"
}

resource "aws_iam_role_policy" "ecs_calm_adapter_service_scheduler_sns" {
  name   = "ecs_task_calm_service_scheduler_sns_policy"
  role   = "${module.ecs_calm_adapter_iam.task_role_name}"
  policy = "${module.service_scheduler_topic.publish_policy}"
}

# Role policies for the transformer

resource "aws_iam_role_policy" "ecs_transformer_task_read_transformer_q" {
  name   = "ecs_transformer_task_read_transformer_q"
  role   = "${module.ecs_transformer_iam.task_role_name}"
  policy = "${module.miro_transformer_queue.read_policy}"
}

resource "aws_iam_role_policy" "ecs_transformer_task_sns" {
  name   = "ecs_task_task_sns_policy"
  role   = "${module.ecs_transformer_iam.task_role_name}"
  policy = "${module.id_minter_topic.publish_policy}"
}

resource "aws_iam_role_policy" "ecs_transformer_task_cloudwatch_metric" {
  name   = "ecs_task_cloudwatch_metric_policy"
  role   = "${module.ecs_transformer_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

# Role policies for the ID minter

resource "aws_iam_role_policy" "ecs_id_minter_task_sns" {
  name   = "ecs_task_task_sns_policy"
  role   = "${module.ecs_id_minter_iam.task_role_name}"
  policy = "${module.es_ingest_topic.publish_policy}"
}

resource "aws_iam_role_policy" "ecs_id_minter_task_read_id_minter_q" {
  name   = "ecs_task_id_minter_policy"
  role   = "${module.ecs_id_minter_iam.task_role_name}"
  policy = "${module.id_minter_queue.read_policy}"
}

resource "aws_iam_role_policy" "id_minter_cloudwatch" {
  role   = "${module.ecs_id_minter_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

# Role policies for the Publish to SNS Lambda

resource "aws_iam_role_policy" "lambda_service_scheduler_sns" {
  name   = "lambda_service_scheduler_sns_policy"
  role   = "${module.lambda_service_scheduler.role_name}"
  policy = "${module.service_scheduler_topic.publish_policy}"
}

resource "aws_iam_role_policy" "lambda_schedule_reindexer_sns" {
  name   = "lambda_schedule_reindexer_sns"
  role   = "${module.lambda_schedule_reindexer.role_name}"
  policy = "${module.service_scheduler_topic.publish_policy}"
}

resource "aws_iam_role_policy" "lambda_schedule_reindexer_dynamo_sns" {
  role   = "${module.lambda_schedule_reindexer.role_name}"
  policy = "${module.dynamo_capacity_topic.publish_policy}"
}

resource "aws_iam_role_policy" "lambda_update_dynamo_capacity_table_access" {
  role   = "${module.lambda_update_dynamo_capacity.role_name}"
  policy = "${data.aws_iam_policy_document.allow_table_capacity_changes.json}"
}

# Role policies for the Update ECS Service Size Lambda

resource "aws_iam_role_policy" "update_ecs_service_size_policy" {
  name   = "lambda_update_ecs_service_size"
  role   = "${module.lambda_update_ecs_service_size.role_name}"
  policy = "${data.aws_iam_policy_document.update_ecs_service_size.json}"
}

# Role policies for the Lambda which updates running tasks when the
# config changes.

resource "aws_iam_role_policy" "update_tasks_for_config_change_policy" {
  role   = "${module.lambda_update_task_for_config_change.role_name}"
  policy = "${data.aws_iam_policy_document.stop_running_tasks.json}"
}

# Role policies for update_service_list lambda

resource "aws_iam_role_policy" "update_service_list_describe_services" {
  role   = "${module.lambda_update_service_list.role_name}"
  policy = "${data.aws_iam_policy_document.describe_services.json}"
}

resource "aws_iam_role_policy" "update_service_list_push_to_s3" {
  role   = "${module.lambda_update_service_list.role_name}"
  policy = "${data.aws_iam_policy_document.s3_put_dashboard_status.json}"
}

resource "aws_iam_role_policy" "update_service_list_read_from_webplatform" {
  role = "${module.lambda_update_service_list.role_name}"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "sts:AssumeRole",
      "Resource": "arn:aws:iam::130871440101:role/platform-team-assume-role"
    }
  ]
}
EOF
}

# Role policies for ecs_ec2_instance_tagger lambda

resource "aws_iam_role_policy" "ecs_ec2_instance_tagger_write_tags" {
  role   = "${module.lambda_ecs_ec2_instance_tagger.role_name}"
  policy = "${data.aws_iam_policy_document.write_ec2_tags.json}"
}

resource "aws_iam_role_policy" "ecs_ec2_instance_tagger_use_tmp" {
  role   = "${module.lambda_ecs_ec2_instance_tagger.role_name}"
  policy = "${data.aws_iam_policy_document.s3_put_infra_tmp.json}"
}

# Role policies for service_deployment_status lambda

resource "aws_iam_role_policy" "service_deployment_status_describe_services" {
  role   = "${module.lambda_service_deployment_status.role_name}"
  policy = "${data.aws_iam_policy_document.describe_services.json}"
}

resource "aws_iam_role_policy" "service_deployment_status_deployments_table" {
  role   = "${module.lambda_service_deployment_status.role_name}"
  policy = "${data.aws_iam_policy_document.deployments_table.json}"
}

# Role policies for notify_old_deploys lambda

resource "aws_iam_role_policy" "notify_old_deploys_sns_publish" {
  role   = "${module.lambda_notify_old_deploys.role_name}"
  policy = "${module.old_deployments.publish_policy}"
}

resource "aws_iam_role_policy" "notify_old_deploys_deployments_table" {
  role   = "${module.lambda_notify_old_deploys.role_name}"
  policy = "${data.aws_iam_policy_document.deployments_table.json}"
}

# Role policies for the miro_reindexer service

resource "aws_iam_role_policy" "reindexer_tracker_table" {
  role   = "${module.ecs_miro_reindexer_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.reindex_tracker_table.json}"
}

resource "aws_iam_role_policy" "reindexer_target_miro" {
  role   = "${module.ecs_miro_reindexer_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.reindex_target_miro.json}"
}

resource "aws_iam_role_policy" "reindexer_cloudwatch" {
  role   = "${module.ecs_miro_reindexer_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

# grafana policies

resource "aws_iam_role_policy" "ecs_grafana_task_cloudwatch_read" {
  name = "ecs_grafana_task_cloudwatch_read"

  # Unfortunately grafana seems to assume the role of the ec2 instance the container is running into.
  # This used to be a bug in grafana which was fixed in version 4.3.0: https://github.com/grafana/grafana/pull/7892
  # Unfortunately we are still seeing this behaviour from the official grafana docker image
  # TODO change to role = "${module.ecs_grafana_iam.task_role_name}"
  role = "${module.ecs_monitoring_iam.instance_role_name}"

  policy = "${data.aws_iam_policy_document.allow_cloudwatch_read_metrics.json}"
}

# Policies for Lamdba that drains ECS container instances when the underlying EC2 instance is terminating

resource "aws_iam_role_policy" "drain_ecs_container_instance_sns" {
  name   = "drain_ecs_container_instance_publish_to_sns"
  role   = "${module.lambda_drain_ecs_container_instance.role_name}"
  policy = "${module.ec2_terminating_topic.publish_policy}"
}

resource "aws_iam_role_policy" "drain_ecs_container_instance_asg_complete" {
  name   = "drain_ecs_container_instance_asg_complete"
  role   = "${module.lambda_drain_ecs_container_instance.role_name}"
  policy = "${data.aws_iam_policy_document.complete_lifecycle_hook.json}"
}

resource "aws_iam_role_policy" "drain_ecs_container_instance_ecs_list" {
  name   = "drain_ecs_container_instance_ecs_list"
  role   = "${module.lambda_drain_ecs_container_instance.role_name}"
  policy = "${data.aws_iam_policy_document.ecs_list_container_tasks.json}"
}

resource "aws_iam_role_policy" "drain_ecs_container_instance_ec2_describe" {
  name   = "drain_ecs_container_instance_ec2_describe"
  role   = "${module.lambda_drain_ecs_container_instance.role_name}"
  policy = "${data.aws_iam_policy_document.ec2_describe_instances.json}"
}

resource "aws_iam_role_policy" "drain_ecs_container_send_asg_heartbeat" {
  name   = "drain_ecs_container_instance_send_asg_heartbeat"
  role   = "${module.lambda_drain_ecs_container_instance.role_name}"
  policy = "${data.aws_iam_policy_document.send_asg_heartbeat.json}"
}

# Policies for dynamo_to_sns lambda to publish to sns topics

resource "aws_iam_role_policy" "dynamo_to_miro_sns" {
  role   = "${module.lambda_dynamo_to_sns.role_name}"
  policy = "${module.miro_transformer_topic.publish_policy}"
}

resource "aws_iam_role_policy" "dynamo_to_calm_sns" {
  role   = "${module.lambda_dynamo_to_sns.role_name}"
  policy = "${module.calm_transformer_topic.publish_policy}"
}

# Policies for gatling script task

resource "aws_iam_role_policy" "gatling_push_to_s3" {
  role   = "${module.ecs_gatling_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.s3_put_gatling_reports.json}"
}

resource "aws_iam_role_policy" "gatling_failure_alarm" {
  role   = "${module.ecs_gatling_iam.task_role_name}"
  policy = "${module.load_test_results.publish_policy}"
}

resource "aws_iam_role_policy" "gatling_results_publication" {
  role   = "${module.ecs_gatling_iam.task_role_name}"
  policy = "${module.load_test_failure_alarm.publish_policy}"
}

# Policies for lambda_gatling_to_cloudwatch

resource "aws_iam_role_policy" "lambda_gatling_to_cloudwatch_put_metric" {
  role   = "${module.lambda_gatling_to_cloudwatch.role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

# Policies for the TIF conversion batch job

resource "aws_iam_role_policy" "batch_tif_conversion_s3_tif_derivative" {
  role   = "${module.batch_tif_conversion_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.s3_tif_derivative.json}"
}

# Policies for the Miro adapter

resource "aws_iam_role_policy" "miro_adapter_read_from_s3" {
  role   = "${module.ecs_miro_adapter_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.s3_read_miro_data.json}"
}

resource "aws_iam_role_policy" "miro_adapter_dynamodb_access" {
  role   = "${module.ecs_miro_adapter_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.reindex_target_miro.json}"
}

# Policies for the Elasticdump task

resource "aws_iam_role_policy" "elasticdump_read_ingestor_config_from_s3" {
  role   = "${module.ecs_elasticdump_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.s3_read_ingestor_config.json}"
}

resource "aws_iam_role_policy" "elasticdump_upload_files_to_s3" {
  role   = "${module.ecs_elasticdump_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.s3_upload_to_to_elasticdump_directory.json}"
}
