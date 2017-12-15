module "id_minter" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v5.0.2"
  name   = "id_minter_${var.name}"

  source_queue_name  = "${module.id_minter_queue.name}"
  source_queue_arn   = "${module.id_minter_queue.arn}"
  ecr_repository_url = "${var.id_minter_repository_url}"
  release_id         = "${var.release_ids["id_minter"]}"

  env_vars = {
    cluster_url = "${var.identifiers_rds_cluster["host"]}"
    db_port     = "${var.identifiers_rds_cluster["port"]}"
    db_username = "${var.identifiers_rds_cluster["username"]}"
    db_password = "${var.identifiers_rds_cluster["password"]}"
    queue_url   = "${module.id_minter_queue.id}"
    topic_arn   = "${module.es_ingest_topic.arn}"
  }

  env_vars_length = 6

  cluster_name = "${var.cluster_name}"
  vpc_id       = "${var.vpc_id}"

  alb_priority = "${random_integer.priority_id_minter.result}"

  alb_cloudwatch_id          = "${var.services_alb["cloudwatch_id"]}"
  alb_listener_https_arn     = "${var.services_alb["listener_https_arn"]}"
  alb_listener_http_arn      = "${var.services_alb["listener_http_arn"]}"
  alb_server_error_alarm_arn = "${var.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${var.alb_client_error_alarm_arn}"
}
