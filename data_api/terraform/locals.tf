locals {
  ec2_terminating_topic_arn                       = "${data.terraform_remote_state.shared_infra.ec2_terminating_topic_arn}"
  ec2_instance_terminating_for_too_long_alarm_arn = "${data.terraform_remote_state.shared_infra.ec2_instance_terminating_for_too_long_alarm_arn}"
  ec2_terminating_topic_publish_policy            = "${data.terraform_remote_state.shared_infra.ec2_terminating_topic_publish_policy}"

  lambda_error_alarm_arn     = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"
  dlq_alarm_arn              = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
  terminal_failure_alarm_arn = "${data.terraform_remote_state.shared_infra.terminal_failure_alarm_arn}"

  es_name     = "${data.terraform_remote_state.catalogue_api.prod_es_name}"
  es_region   = "${data.terraform_remote_state.catalogue_api.prod_es_region}"
  es_port     = "${data.terraform_remote_state.catalogue_api.prod_es_port}"
  es_index    = "${data.terraform_remote_state.catalogue_api.prod_es_index}"
  es_doc_type = "${data.terraform_remote_state.catalogue_api.prod_es_doc_type}"
  es_username = "${data.terraform_remote_state.catalogue_api.prod_es_username}"
  es_password = "${data.terraform_remote_state.catalogue_api.prod_es_password}"

  alb_server_error_alarm_arn = "${data.terraform_remote_state.shared_infra.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${data.terraform_remote_state.shared_infra.alb_client_error_alarm_arn}"

  bucket_alb_logs_id = "${data.terraform_remote_state.shared_infra.bucket_alb_logs_id}"
}
