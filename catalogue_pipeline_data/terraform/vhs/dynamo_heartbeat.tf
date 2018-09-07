module "heartbeat" {
  source = "../../../shared_infra/dynamo_write_heartbeat"

  name               = "${aws_dynamodb_table.table.name}-heartbeat"
  dynamo_table_names = ["${aws_dynamodb_table.table.name}"]

  infra_bucket           = "${var.infra_bucket}"
  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
}
