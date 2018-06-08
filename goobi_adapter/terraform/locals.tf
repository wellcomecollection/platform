locals {
  alb_server_error_alarm_arn = "${data.terraform_remote_state.shared_infra.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${data.terraform_remote_state.shared_infra.alb_client_error_alarm_arn}"

  ec2_terminating_topic_arn                       = "${data.terraform_remote_state.shared_infra.ec2_terminating_topic_arn}"
  ec2_instance_terminating_for_too_long_alarm_arn = "${data.terraform_remote_state.shared_infra.ec2_instance_terminating_for_too_long_alarm_arn}"
  ec2_terminating_topic_publish_policy            = "${data.terraform_remote_state.shared_infra.ec2_terminating_topic_publish_policy}"

  alb_log_bucket_id = "${data.terraform_remote_state.shared_infra.bucket_alb_logs_id}"

  vhs_goobi_full_access_policy = "${data.terraform_remote_state.catalogue_pipeline.vhs_goobi_full_access_policy}"
  vhs_goobi_table_name         = "${data.terraform_remote_state.catalogue_pipeline.vhs_goobi_table_name}"
  vhs_goobi_bucket_name        = "${data.terraform_remote_state.catalogue_pipeline.vhs_goobi_bucket_name}"
}
