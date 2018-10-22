locals {
  cloudfront_logs_bucket_domain_name = "${data.terraform_remote_state.shared_infra.cloudfront_logs_bucket_domain_name}"
  
  miro_topic_arn = "${data.terraform_remote_state.shared_infra.reporting_miro_reindex_topic_arn}"

  sierra_topic_arn = "${data.terraform_remote_state.shared_infra.reporting_sierra_reindex_topic_arn}"

  lambda_error_alarm_arn = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"

  infra_bucket = "${data.terraform_remote_state.shared_infra.infra_bucket}"
}
