data "template_file" "es_cluster_host_ingestor" {
  template = "$${name}.$${region}.aws.found.io"

  vars {
    name   = "${var.es_config_ingestor["name"]}"
    region = "${var.es_config_ingestor["region"]}"
  }
}

module "ingestor" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v6.4.0"
  name   = "ingestor"

  source_queue_name  = "${module.es_ingest_queue.name}"
  source_queue_arn   = "${module.es_ingest_queue.arn}"
  ecr_repository_url = "${module.ecr_repository_ingestor.repository_url}"
  release_id         = "${var.release_ids["ingestor"]}"

  env_vars = {
    es_host           = "${data.template_file.es_cluster_host_ingestor.rendered}"
    es_port           = "${var.es_config_ingestor["port"]}"
    es_name           = "${var.es_config_ingestor["name"]}"
    es_index          = "${var.es_config_ingestor["index"]}"
    es_doc_type       = "${var.es_config_ingestor["doc_type"]}"
    es_username       = "${var.es_config_ingestor["username"]}"
    es_password       = "${var.es_config_ingestor["password"]}"
    es_protocol       = "${var.es_config_ingestor["protocol"]}"
    ingest_queue_id   = "${module.es_ingest_queue.id}"
    metrics_namespace = "ingestor"
  }

  memory = "2048"
  cpu    = "512"

  env_vars_length = 10

  alb_priority = 107

  cluster_name               = "${module.catalogue_pipeline_cluster.cluster_name}"
  vpc_id                     = "${module.vpc_services.vpc_id}"
  alb_cloudwatch_id          = "${module.catalogue_pipeline_cluster.alb_cloudwatch_id}"
  alb_listener_https_arn     = "${module.catalogue_pipeline_cluster.alb_listener_https_arn}"
  alb_listener_http_arn      = "${module.catalogue_pipeline_cluster.alb_listener_http_arn}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"

  enable_alb_alarm = false
}
