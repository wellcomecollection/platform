module "id_minter" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=nicer-service-vars"
  name   = "id_minter_${var.name}"

  source_queue_name  = "${module.id_minter_queue.name}"
  source_queue_arn   = "${module.id_minter_queue.arn}"
  ecr_repository_url = "${var.id_minter_repository_url}"
  release_id         = "${var.release_ids["id_minter"]}"

  config_vars = [
    {
      name  = "cluster_url"
      value = "${var.identifiers_rds_cluster["host"]}"
    },
    {
      name  = "db_port"
      value = "${var.identifiers_rds_cluster["port"]}"
    },
    {
      name  = "db_username"
      value = "${var.identifiers_rds_cluster["username"]}"
    },
    {
      name  = "db_password"
      value = "${var.identifiers_rds_cluster["password"]}"
    },
    {
      name  = "queue_url"
      value = "${module.id_minter_queue.id}"
    },
    {
      name  = "topic_arn"
      value = "${module.es_ingest_topic.arn}"
    },
  ]

  cluster_name = "${var.cluster_name}"
  vpc_id       = "${var.vpc_id}"

  alb_priority = "${random_integer.priority_id_minter.result}"

  alb_cloudwatch_id          = "${var.services_alb["cloudwatch_id"]}"
  alb_listener_https_arn     = "${var.services_alb["listener_https_arn"]}"
  alb_listener_http_arn      = "${var.services_alb["listener_http_arn"]}"
  alb_server_error_alarm_arn = "${var.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${var.alb_client_error_alarm_arn}"
}
