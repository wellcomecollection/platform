locals {
  gateway_server_error_alarm_arn = data.terraform_remote_state.shared_infra.outputs.gateway_server_error_alarm_arn
  lambda_error_alarm_arn         = data.terraform_remote_state.shared_infra.outputs.lambda_error_alarm_arn
  dlq_alarm_arn                  = data.terraform_remote_state.shared_infra.outputs.dlq_alarm_arn

  bucket_alb_logs_id = data.terraform_remote_state.shared_infra.outputs.bucket_alb_logs_id

  cloudfront_errors_topic_arn = data.terraform_remote_state.loris.outputs.cloudfront_errors_topic_arn

  namespace  = "monitoring"

  vpc_id          = data.terraform_remote_state.shared_infra.outputs.monitoring_vpc_delta_id
  private_subnets = data.terraform_remote_state.shared_infra.outputs.monitoring_vpc_delta_private_subnets
  public_subnets  = data.terraform_remote_state.shared_infra.outputs.monitoring_vpc_delta_public_subnets
}
