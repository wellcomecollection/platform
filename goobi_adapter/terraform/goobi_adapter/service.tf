locals {
  service_name = "goobi_reader"
}

module "goobi_reader_service" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v10.2.2"
  name   = "${local.service_name}"

  ecr_repository_url = "${var.ecr_repository_url}"
  release_id         = "${var.release_id}"

  source_queue_name = "${module.goobi_mets_queue.name}"
  source_queue_arn  = "${module.goobi_mets_queue.arn}"

  env_vars = {
    goobi_mets_queue_url  = "${module.goobi_mets_queue.id}"
    metrics_namespace      = "${local.service_name}"
    vhs_goobi_tablename    = "${var.vhs_goobi_tablename}"
    vhs_goobi_bucketname   = "${var.vhs_goobi_bucketname}"
  }

  env_vars_length = 4

  cpu    = 512
  memory = 2048

  cluster_name = "${var.cluster_name}"

  vpc_id = "${var.vpc_id}"

  alb_cloudwatch_id          = "${var.alb_cloudwatch_id}"
  alb_listener_https_arn     = "${var.alb_listener_https_arn}"
  alb_listener_http_arn      = "${var.alb_listener_http_arn}"
  alb_server_error_alarm_arn = "${var.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${var.alb_client_error_alarm_arn}"

  enable_alb_alarm = false

  max_capacity = 1

  log_retention_in_days = 30
}
