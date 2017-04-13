module "transformer_config" {
  source       = "./app_config"
  app_name     = "transformer"
  infra_bucket = "${var.infra_bucket}"

  template_vars = {
    stream_arn = "${aws_dynamodb_table.calm_table.stream_arn}"
    sns_arn    = "${aws_sns_topic.id_minter_topic.arn}"
  }
}

module "calm_adapter_config" {
  source       = "./app_config"
  app_name     = "calm_adapter"
  infra_bucket = "${var.infra_bucket}"

  template_vars = {
    table_name = "${aws_dynamodb_table.calm_table.name}"
    sns_arn    = "${aws_sns_topic.service_scheduler_topic.arn}"
  }
}
