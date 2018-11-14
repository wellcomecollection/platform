locals {
  cloudfront_logs_bucket_domain_name = "${data.terraform_remote_state.shared_infra.cloudfront_logs_bucket_domain_name}"

  miro_topic_arn = "${data.terraform_remote_state.shared_infra.reporting_miro_reindex_topic_arn}"

  miro_inventory_topic_arn = "${data.terraform_remote_state.shared_infra.reporting_miro_inventory_reindex_topic_arn}"

  sierra_topic_arn = "${data.terraform_remote_state.shared_infra.reporting_sierra_reindex_topic_arn}"

  lambda_error_alarm_arn = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"

  infra_bucket = "${data.terraform_remote_state.shared_infra.infra_bucket}"

  miro_vhs_read_policy = "${data.terraform_remote_state.infra_critical.vhs_miro_read_policy}"

  miro_inventory_vhs_read_policy = "${data.terraform_remote_state.infra_critical.vhs_miro_inventory_read_policy}"
  
  sierra_vhs_read_policy         = "${data.terraform_remote_state.infra_critical.vhs_sierra_read_policy}"
}
