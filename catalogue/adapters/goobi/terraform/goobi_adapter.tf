module "goobi_adapter" {
  source    = "./goobi_adapter"
  namespace = "goobi_adapter"

  goobi_mets_queue_name  = "goobi_mets"
  goobi_mets_bucket_name = "${aws_s3_bucket.goobi_adapter.id}"
  goobi_mets_topic       = "${aws_sns_topic.goobi_notifications_topic.name}"

  dlq_alarm_arn = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"

  vpc_id = "${local.vpc_id}"

  account_id = "${data.aws_caller_identity.current.account_id}"

  vhs_goobi_tablename          = "${local.vhs_goobi_table_name}"
  vhs_goobi_bucketname         = "${local.vhs_goobi_bucket_name}"
  vhs_goobi_full_access_policy = "${local.vhs_goobi_full_access_policy}"

  container_image = "${local.goobi_reader_container_image}"
  subnets         = "${local.private_subnets}"
}
