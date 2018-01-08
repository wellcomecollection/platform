locals {
  ec2_terminating_topic_arn                       = "${data.terraform_remote_state.shared_infra.ec2_terminating_topic_arn}"
  ec2_instance_terminating_for_too_long_alarm_arn = "${data.terraform_remote_state.shared_infra.ec2_instance_terminating_for_too_long_alarm_arn}"
  ec2_terminating_topic_publish_policy            = "${data.terraform_remote_state.shared_infra.ec2_terminating_topic_publish_policy}"

  alb_server_error_alarm_arn = "${data.terraform_remote_state.shared_infra.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${data.terraform_remote_state.shared_infra.alb_client_error_alarm_arn}"

  bucket_alb_logs_id = "${data.terraform_remote_state.shared_infra.bucket_alb_logs_id}"

  run_ecs_task_topic_arn = "${data.terraform_remote_state.shared_infra.run_ecs_task_topic_arn}"

  lambda_error_alarm_arn = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"

  service_scheduler_topic_name = "${data.terraform_remote_state.shared_infra.service_scheduler_topic_name}"
}

data "aws_s3_bucket" "infra" {
  bucket = "${var.infra_bucket}"
}

data "aws_sns_topic" "service_scheduler_topic" {
  name = "${local.service_scheduler_topic_name}"
}
