data "template_file" "es_cluster_host_snapshot" {
  template = "$${name}.$${region}.aws.found.io"

  vars {
    name   = "${var.es_config_snapshot["name"]}"
    region = "${var.es_config_snapshot["region"]}"
  }
}

module "snapshot_convertor" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v8.0.0"
  name   = "snapshot_convertor"

  source_queue_name = "${module.snapshot_convertor_queue.name}"
  source_queue_arn  = "${module.snapshot_convertor_queue.arn}"

  ecr_repository_url = "${module.ecr_repository_snapshot_convertor.repository_url}"
  release_id         = "${var.release_ids["snapshot_convertor"]}"

  env_vars = {
    queue_url = "${module.snapshot_convertor_queue.id}"
    topic_arn = "${module.snapshot_conversion_complete_topic.arn}"
    es_host           = "${data.template_file.es_cluster_host_snapshot.rendered}"
    es_port           = "${var.es_config_snapshot["port"]}"
    es_name           = "${var.es_config_snapshot["name"]}"
    es_index_v1       = "${var.es_config_snapshot["index_v1"]}"
    es_index_v2       = "${var.es_config_snapshot["index_v2"]}"
    es_doc_type       = "${var.es_config_snapshot["doc_type"]}"
    es_username       = "${var.es_config_snapshot["username"]}"
    es_password       = "${var.es_config_snapshot["password"]}"
    es_protocol       = "${var.es_config_snapshot["protocol"]}"
  }

  env_vars_length = 11

  memory = 2048
  cpu    = 512

  cluster_name = "${module.data_api_cluster.cluster_name}"
  vpc_id       = "${module.vpc_data_api.vpc_id}"

  alb_cloudwatch_id          = "${module.data_api_cluster.alb_cloudwatch_id}"
  alb_listener_https_arn     = "${module.data_api_cluster.alb_listener_https_arn}"
  alb_listener_http_arn      = "${module.data_api_cluster.alb_listener_http_arn}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"

  enable_alb_alarm = false

  max_capacity = 15
}
