module "snapshot_scheduler" {
  source = "snapshot_scheduler"

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
  infra_bucket           = "${var.infra_bucket}"
}

module "elasticdump" {
  source = "elasticdump"

  upload_bucket       = "${var.infra_bucket}"
  schedule_topic_name = "${module.snapshot_scheduler.topic_name}"

  es_name = "${local.es_name}"
  es_region = "${local.es_region}"
  es_port = "${local.es_port}"
  es_index = "${local.es_index}"
  es_doc_type = "${local.es_doc_type}"
  es_username = "${local.es_username}"
  es_password = "${local.es_password}"

  dlq_alarm_arn = "${local.dlq_alarm_arn}"

  aws_region = "${var.aws_region}"
  account_id = "${data.aws_caller_identity.current.account_id}"

  release_ids = "${var.release_ids}"

  cluster_name = "${local.cluster_name}"
  vpc_id = "${local.vpc_id}"

  alb_cloudwatch_id = "${local.alb_cloudwatch_id}"
  alb_listener_https_arn = "${local.alb_listener_https_arn}"
  alb_listener_http_arn = "${local.alb_listener_http_arn}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
}
