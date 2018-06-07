module "goobi_adapter" {
  source                  = "./goobi_adapter"
  goobi_mets_queue_name  = "goobi_mets"
  goobi_mets_bucket_name = "${aws_s3_bucket.goobi_adapter.id}"
  goobi_mets_topic       = "${aws_sns_topic.goobi_notifications_topic.name}"

  release_id         = "${var.release_ids["goobi_reader"]}"
  ecr_repository_url = "${module.ecr_repository_goobi_reader.repository_url}"

  dlq_alarm_arn = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"

  vpc_id = "${module.vpc_goobi_adapter.vpc_id}"

  account_id = "${data.aws_caller_identity.current.account_id}"

  cluster_name = "${module.goobi_adapter_cluster.cluster_name}"

  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_cloudwatch_id          = "${module.goobi_adapter_cluster.alb_cloudwatch_id}"
  alb_listener_http_arn      = "${module.goobi_adapter_cluster.alb_listener_http_arn}"
  alb_listener_https_arn     = "${module.goobi_adapter_cluster.alb_listener_https_arn}"

  vhs_goobi_tablename           = "${local.vhs_goobi_table_name}"
  vhs_goobi_bucketname          = "${local.vhs_goobi_bucket_name}"
  vhs_goobi_full_access_policy  = "${local.vhs_goobi_full_access_policy}"
}
