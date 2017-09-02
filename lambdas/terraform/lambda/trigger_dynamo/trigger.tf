resource "aws_lambda_event_source_mapping" "dynamo_update_mapping" {
  batch_size        = "${var.batch_size}"
  event_source_arn  = "${var.stream_arn}"
  function_name     = "${var.function_arn}"
  starting_position = "${var.starting_position}"
}
