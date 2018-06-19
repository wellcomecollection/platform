data "template_file" "es_cluster_host_snapshot" {
  template = "$${name}.$${region}.aws.found.io"

  vars {
    name   = "${var.es_cluster_credentials["name"]}"
    region = "${var.es_cluster_credentials["region"]}"
  }
}

module "snapshot_generator" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v10.3.0"
  name   = "snapshot_generator"

  source_queue_name = "${module.snapshot_generator_queue.name}"
  source_queue_arn  = "${module.snapshot_generator_queue.arn}"

  ecr_repository_url = "${module.ecr_repository_snapshot_generator.repository_url}"
  release_id         = "${var.release_ids["snapshot_generator"]}"

  env_vars = {
    queue_url         = "${module.snapshot_generator_queue.id}"
    topic_arn         = "${module.snapshot_generation_complete_topic.arn}"
    es_host           = "${data.template_file.es_cluster_host_snapshot.rendered}"
    es_port           = "${var.es_cluster_credentials["port"]}"
    es_username       = "${var.es_cluster_credentials["username"]}"
    es_password       = "${var.es_cluster_credentials["password"]}"
    es_protocol       = "${var.es_cluster_credentials["protocol"]}"
    es_index_v1       = "${var.es_config_snapshot["index_v1"]}"
    es_index_v2       = "${var.es_config_snapshot["index_v2"]}"
    es_doc_type       = "${var.es_config_snapshot["doc_type"]}"
    metrics_namespace = "snapshot_generator"
  }

  memory = 3072
  cpu    = 2048

  cluster_name = "${module.data_api_cluster.cluster_name}"
  vpc_id       = "${module.vpc_data_api.vpc_id}"

  alb_cloudwatch_id          = "${module.data_api_cluster.alb_cloudwatch_id}"
  alb_listener_https_arn     = "${module.data_api_cluster.alb_listener_https_arn}"
  alb_listener_http_arn      = "${module.data_api_cluster.alb_listener_http_arn}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"

  enable_alb_alarm = false

  max_capacity = 2

  scale_down_period_in_minutes = 30

  log_retention_in_days = 30
}
