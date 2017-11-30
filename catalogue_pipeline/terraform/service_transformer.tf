module "transformer_appautoscaling" {
  source = "git::https://github.com/wellcometrust/terraform.git//autoscaling/app/ecs?ref=v1.1.0"
  name   = "transformer"

  cluster_name = "${aws_ecs_cluster.services.name}"
  service_name = "${module.transformer.service_name}"
}

module "transformer_sqs_autoscaling_alarms" {
  source = "git::https://github.com/wellcometrust/terraform.git//autoscaling/alarms/sqs?ref=v1.1.0"
  name   = "transformer"

  queue_name = "${module.miro_transformer_queue.name}"

  scale_up_arn   = "${module.transformer_appautoscaling.scale_up_arn}"
  scale_down_arn = "${module.transformer_appautoscaling.scale_down_arn}"
}

module "transformer" {
  source             = "git::https://github.com/wellcometrust/terraform.git//services?ref=v1.3.0"
  name               = "transformer"
  cluster_id         = "${aws_ecs_cluster.services.id}"
  task_role_arn      = "${module.ecs_transformer_iam.task_role_arn}"
  vpc_id             = "${module.vpc_services.vpc_id}"
  app_uri            = "${module.ecr_repository_transformer.repository_url}:${var.release_ids["transformer"]}"
  listener_https_arn = "${module.services_alb.listener_https_arn}"
  listener_http_arn  = "${module.services_alb.listener_http_arn}"
  path_pattern       = "/transformer/*"
  alb_priority       = "100"
  healthcheck_path   = "/transformer/management/healthcheck"
  infra_bucket       = "${var.infra_bucket}"

  config_key           = "config/${var.build_env}/transformer.ini"
  config_template_path = "config/transformer.ini.template"

  cpu    = 256
  memory = 1024

  deployment_minimum_healthy_percent = "0"
  deployment_maximum_percent         = "200"

  config_vars = {
    sns_arn              = "${module.id_minter_topic.arn}"
    transformer_queue_id = "${module.miro_transformer_queue.id}"
    source_table_name    = "${aws_dynamodb_table.miro_table.name}"
    metrics_namespace    = "miro-transformer"
  }

  loadbalancer_cloudwatch_id   = "${module.services_alb.cloudwatch_id}"
  server_error_alarm_topic_arn = "${local.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${local.alb_client_error_alarm_arn}"

  https_domain = "services.wellcomecollection.org"
}
