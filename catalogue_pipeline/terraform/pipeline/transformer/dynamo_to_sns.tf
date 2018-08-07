module "dynamo_to_sns" {
  source = "../../../../shared_infra/dynamo_to_sns"

  name           = "${var.namespace}_${var.source_name}"
  src_stream_arn = "${var.vhs_table_stream_arn}"
  dst_topic_arn  = "${module.transformer_topic.arn}"

  stream_view_type = "NEW_IMAGE_ONLY"

  lambda_error_alarm_arn = "${var.lambda_error_alarm_arn}"

  infra_bucket = "${var.infra_bucket}"
}
