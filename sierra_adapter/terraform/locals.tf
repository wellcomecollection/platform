locals {
  alb_cloudwatch_id      = "${module.sierra_adapter_cluster.alb_cloudwatch_id}"
  alb_listener_http_arn  = "${module.sierra_adapter_cluster.alb_listener_http_arn}"
  alb_listener_https_arn = "${module.sierra_adapter_cluster.alb_listener_https_arn}"

  alb_server_error_alarm_arn = "${data.terraform_remote_state.shared_infra.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${data.terraform_remote_state.shared_infra.alb_client_error_alarm_arn}"
  lambda_error_alarm_arn     = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"

  alb_log_bucket_id = "${data.terraform_remote_state.shared_infra.bucket_alb_logs_id}"

  ec2_terminating_topic_arn                       = "${data.terraform_remote_state.shared_infra.ec2_terminating_topic_arn}"
  ec2_instance_terminating_for_too_long_alarm_arn = "${data.terraform_remote_state.shared_infra.ec2_instance_terminating_for_too_long_alarm_arn}"
  ec2_terminating_topic_publish_policy            = "${data.terraform_remote_state.shared_infra.ec2_terminating_topic_publish_policy}"
}
