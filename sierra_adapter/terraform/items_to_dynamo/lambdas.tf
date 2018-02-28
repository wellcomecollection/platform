module "dynamo_to_sns" {
  source = "../../../shared_infra/dynamo_to_sns"

  name           = "sierra_items_updates"
  src_stream_arn = "${aws_dynamodb_table.sierra_table.stream_arn}"
  dst_topic_arn  = "${module.sierra_to_dynamo_updates_topic.arn}"

  infra_bucket = "${var.infra_bucket}"

  stream_view_type = "NEW_IMAGE_ONLY"

  lambda_error_alarm_arn = "${var.lambda_error_alarm_arn}"
}
