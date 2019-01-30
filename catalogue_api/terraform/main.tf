module "catalogue_api" {
  source = "catalogue_api"

  namespace = "catalogue_api_gw"

  vpc_id  = "${local.vpc_id}"
  subnets = ["${local.private_subnets}"]

  container_port = "8888"
  cluster_name   = "${aws_ecs_cluster.cluster.name}"

  remus_container_image = "${local.remus_app_uri}"
  remus_es_config       = "${local.remus_es_config}"
  remus_task_number     = "${local.remus_task_number}"

  romulus_container_image = "${local.romulus_app_uri}"
  romulus_es_config       = "${local.romulus_es_config}"
  romulus_task_number     = "${local.romulus_task_number}"

  nginx_container_image = "${local.nginx_container_uri}"

  production_api = "${local.production_api}"
  stage_api      = "${local.stage_api}"

  alarm_topic_arn = "${local.gateway_server_error_alarm_arn}"
}

module "data_api" {
  source = "data_api"

  aws_region   = "${var.aws_region}"
  infra_bucket = "${local.infra_bucket}"

  es_config_snapshot = "${local.prod_es_config}"

  snapshot_generator_release_uri = "${local.snapshot_generator_release_uri}"

  critical_slack_webhook = ""

  vpc_id          = "${local.vpc_id}"
  private_subnets = ["${local.private_subnets}"]
}

module "api_docs" {
  source = "api_docs"

  update_api_docs_release_uri = "${local.update_api_docs_release_uri}"
}
