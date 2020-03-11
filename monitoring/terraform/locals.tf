locals {
  gateway_server_error_alarm_arn = "${data.terraform_remote_state.shared_infra.gateway_server_error_alarm_arn}"
  lambda_error_alarm_arn         = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"
  dlq_alarm_arn                  = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"

  bucket_alb_logs_id = "${data.terraform_remote_state.shared_infra.bucket_alb_logs_id}"

  cloudfront_errors_topic_arn = "${data.terraform_remote_state.loris.cloudfront_errors_topic_arn}"

  namespace  = "monitoring"

  vpc_id          = "${data.terraform_remote_state.shared_infra.monitoring_vpc_delta_id}"
  private_subnets = "${data.terraform_remote_state.shared_infra.monitoring_vpc_delta_private_subnets}"
  public_subnets  = "${data.terraform_remote_state.shared_infra.monitoring_vpc_delta_public_subnets}"
}
