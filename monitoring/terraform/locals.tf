locals {
  ec2_terminating_topic_arn                       = "${data.terraform_remote_state.shared_infra.ec2_terminating_topic_arn}"
  ec2_instance_terminating_for_too_long_alarm_arn = "${data.terraform_remote_state.shared_infra.ec2_instance_terminating_for_too_long_alarm_arn}"
  ec2_terminating_topic_publish_policy            = "${data.terraform_remote_state.shared_infra.ec2_terminating_topic_publish_policy}"

  alb_server_error_alarm_arn = "${data.terraform_remote_state.shared_infra.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${data.terraform_remote_state.shared_infra.alb_client_error_alarm_arn}"
  lambda_error_alarm_arn     = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"
  terminal_failure_alarm_arn = "${data.terraform_remote_state.shared_infra.terminal_failure_alarm_arn}"
  dlq_alarm_arn              = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"

  bucket_alb_logs_id = "${data.terraform_remote_state.shared_infra.bucket_alb_logs_id}"

  terraform_apply_topic_name  = "${data.terraform_remote_state.shared_infra.terraform_apply_topic_name}"
  cloudfront_errors_topic_arn = "${data.terraform_remote_state.loris.cloudfront_errors_topic_arn}"

  namespace  = "monitoring"
  account_id = "${data.aws_caller_identity.current.account_id}"

  vpc_id          = "${data.terraform_remote_state.shared_infra.monitoring_vpc_delta_id}"
  private_subnets = "${data.terraform_remote_state.shared_infra.monitoring_vpc_delta_private_subnets}"
  public_subnets  = "${data.terraform_remote_state.shared_infra.monitoring_vpc_delta_public_subnets}"
}
