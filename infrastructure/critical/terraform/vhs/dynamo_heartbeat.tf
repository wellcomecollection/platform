module "heartbeat" {
  source = "../dynamo_write_heartbeat"

  name               = "${local.table_name}-heartbeat"
  dynamo_table_names = ["${local.table_name}"]

  infra_bucket           = "${var.infra_bucket}"
  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
}
