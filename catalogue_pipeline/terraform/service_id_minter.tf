module "id_minter" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v2.0.0"
  name   = "id_minter"

  source_queue_name  = "${module.id_minter_queue.name}"
  source_queue_arn   = "${module.id_minter_queue.arn}"
  ecr_repository_url = "${module.ecr_repository_id_minter.repository_url}"
  release_id         = "${var.release_ids["id_minter"]}"

  config_vars = {
    rds_database_name   = "${module.identifiers_rds_cluster.database_name}"
    rds_host            = "${module.identifiers_rds_cluster.host}"
    rds_port            = "${module.identifiers_rds_cluster.port}"
    rds_username        = "${module.identifiers_rds_cluster.username}"
    rds_password        = "${module.identifiers_rds_cluster.password}"
    id_minter_queue_id  = "${module.id_minter_queue.id}"
    es_ingest_topic_arn = "${module.es_ingest_topic.arn}"
    metrics_namespace   = "id-minter"
  }

  alb_priority = "103"

  cluster_name               = "${aws_ecs_cluster.services.name}"
  vpc_id                     = "${module.vpc_services.vpc_id}"
  alb_cloudwatch_id          = "${module.services_alb.cloudwatch_id}"
  alb_listener_https_arn     = "${module.services_alb.listener_https_arn}"
  alb_listener_http_arn      = "${module.services_alb.listener_http_arn}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
}
