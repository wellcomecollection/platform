locals {
  es_config_ingestor = {
    index_v1 = "v1-2018-06-29-identity-state"
    index_v2 = "v2-2018-06-29-identity-state"
    doc_type = "work"
  }
}

data "template_file" "es_cluster_host_ingestor" {
  template = "$${name}.$${region}.aws.found.io"

  vars {
    name   = "${var.es_cluster_credentials["name"]}"
    region = "${var.es_cluster_credentials["region"]}"
  }
}

module "ingestor" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v10.3.0"
  name   = "ingestor"

  source_queue_name  = "${module.es_ingest_queue.name}"
  source_queue_arn   = "${module.es_ingest_queue.arn}"
  ecr_repository_url = "${module.ecr_repository_ingestor.repository_url}"
  release_id         = "${var.release_ids["ingestor"]}"

  env_vars = {
    es_host             = "${data.template_file.es_cluster_host_ingestor.rendered}"
    es_port             = "${var.es_cluster_credentials["port"]}"
    es_username         = "${var.es_cluster_credentials["username"]}"
    es_password         = "${var.es_cluster_credentials["password"]}"
    es_protocol         = "${var.es_cluster_credentials["protocol"]}"
    es_index_v1         = "${local.es_config_ingestor["index_v1"]}"
    es_index_v2         = "${local.es_config_ingestor["index_v2"]}"
    es_doc_type         = "${local.es_config_ingestor["doc_type"]}"
    ingest_queue_id     = "${module.es_ingest_queue.id}"
    message_bucket_name = "${aws_s3_bucket.messages.id}"
    metrics_namespace   = "ingestor"
  }

  memory = 2048
  cpu    = 1024

  alb_priority = 107

  cluster_name               = "${module.catalogue_pipeline_cluster.cluster_name}"
  vpc_id                     = "${module.vpc_services.vpc_id}"
  alb_cloudwatch_id          = "${module.catalogue_pipeline_cluster.alb_cloudwatch_id}"
  alb_listener_https_arn     = "${module.catalogue_pipeline_cluster.alb_listener_https_arn}"
  alb_listener_http_arn      = "${module.catalogue_pipeline_cluster.alb_listener_http_arn}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"

  enable_alb_alarm = false

  max_capacity = 15

  log_retention_in_days = 30
}
