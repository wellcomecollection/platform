module "ingestor_appautoscaling" {
  source  = "git::https://github.com/wellcometrust/terraform.git//autoscaling/app/ecs?ref=ecs-sqs-autoscaling-policy"
  name    = "ingestor"

  cluster_name = "${aws_ecs_cluster.services.name}"
  service_name = "${module.ingestor.service_name}"
}

module "ingestor_sqs_autoscaling_alarms" {
  source  = "git::https://github.com/wellcometrust/terraform.git//autoscaling/alarms/sqs?ref=ecs-sqs-autoscaling-policy"
  name    = "ingestor"

  queue_name   = "${module.es_ingest_queue.id}"

  scale_up_arn = "${module.ingestor_appautoscaling.scale_up_arn}"
  scale_down_arn = "${module.ingestor_appautoscaling.scale_down_arn}"
}

module "ingestor" {
  source             = "git::https://github.com/wellcometrust/terraform.git//services?ref=v1.0.0"
  name               = "ingestor"
  cluster_id         = "${aws_ecs_cluster.services.id}"
  task_role_arn      = "${module.ecs_ingestor_iam.task_role_arn}"
  vpc_id             = "${module.vpc_services.vpc_id}"
  app_uri            = "${module.ecr_repository_ingestor.repository_url}:${var.release_ids["ingestor"]}"
  nginx_uri          = "${module.ecr_repository_nginx_services.repository_url}:${var.release_ids["nginx_services"]}"
  listener_https_arn = "${module.services_alb.listener_https_arn}"
  listener_http_arn  = "${module.services_alb.listener_http_arn}"
  path_pattern       = "/ingestor/*"
  alb_priority       = "102"
  healthcheck_path   = "/ingestor/management/healthcheck"
  infra_bucket       = "${var.infra_bucket}"
  config_key         = "config/${var.build_env}/ingestor.ini"

  cpu    = 256
  memory = 1024

  deployment_minimum_healthy_percent = "0"
  deployment_maximum_percent         = "200"

  config_vars = {
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

  loadbalancer_cloudwatch_id   = "${module.services_alb.cloudwatch_id}"
  server_error_alarm_topic_arn = "${local.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${local.alb_client_error_alarm_arn}"
}

data "template_file" "es_cluster_host_ingestor" {
  template = "$${name}.$${region}.aws.found.io"

  vars {
    name   = "${var.es_config_ingestor["name"]}"
    region = "${var.es_config_ingestor["region"]}"
  }
}