module "id_minter" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v2.0.0"
  name   = "id_minter_${var.name}"

  source_queue_name  = "${module.id_minter_queue.name}"
  source_queue_arn   = "${module.id_minter_queue.arn}"
  ecr_repository_url = "${var.id_minter_repository_url}"
  release_id         = "${var.release_ids["id_minter"]}"
  config_template = "id_minter"

  config_vars = {
    rds_database_name   = "${var.identifiers_rds_cluster["database_name"]}"
    rds_host            = "${var.identifiers_rds_cluster["host"]}"
    rds_port            = "${var.identifiers_rds_cluster["port"]}"
    rds_username        = "${var.identifiers_rds_cluster["username"]}"
    rds_password        = "${var.identifiers_rds_cluster["password"]}"
    id_minter_queue_id  = "${module.id_minter_queue.id}"
    es_ingest_topic_arn = "${module.es_ingest_topic.arn}"
    metrics_namespace   = "id-minter"
  }

  alb_priority = "${random_integer.priority_id_minter.result}"

  cluster_name               = "${var.cluster_name}"
  vpc_id                     = "${var.vpc_id}"
  alb_cloudwatch_id          = "${var.services_alb["cloudwatch_id"]}"
  alb_listener_https_arn     = "${var.services_alb["listener_https_arn"]}"
  alb_listener_http_arn      = "${var.services_alb["listener_http_arn"]}"
  alb_server_error_alarm_arn = "${var.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${var.alb_client_error_alarm_arn}"
}
