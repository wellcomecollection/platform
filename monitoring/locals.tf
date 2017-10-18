locals {
  ec2_terminating_topic_arn                       = "${data.terraform_remote_state.shared_infra.ec2_terminating_topic_arn}"
  ec2_instance_terminating_for_too_long_alarm_arn = "${data.terraform_remote_state.shared_infra.ec2_instance_terminating_for_too_long_alarm_arn}"
  ec2_terminating_topic_publish_policy            = "${data.terraform_remote_state.shared_infra.ec2_terminating_topic_publish_policy}"

  alb_server_error_alarm_arn = "${data.terraform_remote_state.shared_infra.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${data.terraform_remote_state.shared_infra.alb_client_error_alarm_arn}"
  lambda_error_alarm_arn     = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"
  terminal_failure_alarm_arn = "${data.terraform_remote_state.shared_infra.terminal_failure_alarm_arn}"
  dlq_alarm_arn              = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"

  bucket_infra_arn = "${data.terraform_remote_state.shared_infra.bucket_infra_arn}"
  bucket_infra_id  = "${data.terraform_remote_state.shared_infra.bucket_infra_id}"

  bucket_alb_logs_id = "${data.terraform_remote_state.shared_infra.bucket_alb_logs_id}"

  aws_ecs_cluster_services_id = "${data.terraform_remote_state.catalogue_pipeline.aws_ecs_cluster_services_id}"
}
