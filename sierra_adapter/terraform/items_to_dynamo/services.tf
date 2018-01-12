data "aws_ecs_cluster" "cluster" {
  cluster_name = "${var.cluster_name}"
}

module "sierra_to_dynamo_service" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//ecs/service?ref=v5.3.0"
  name   = "sierra_items_to_dynamo"

  app_uri = "${module.ecr_repository_sierra_to_dynamo.repository_url}:${var.release_id}"

  env_vars = {
    demultiplexer_queue_url = "${module.demultiplexer_queue.id}"
    metrics_namespace = "sierra_items_to_dynamo"

    dynamo_table_name = "${aws_dynamodb_table.sierra_table.id}"
  }
  env_vars_length = 3

  path_pattern = "/sierra_items_to_dynamo/*"

  cpu    = 256
  memory = 1024

  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 200

  cluster_id                   = "${data.aws_ecs_cluster.cluster.arn}"
  vpc_id                       = "${var.vpc_id}"
  loadbalancer_cloudwatch_id   = "${var.alb_cloudwatch_id}"
  listener_https_arn           = "${var.alb_listener_https_arn}"
  listener_http_arn            = "${var.alb_listener_http_arn}"
  client_error_alarm_topic_arn = "${var.alb_server_error_alarm_arn}"
  server_error_alarm_topic_arn = "${var.alb_client_error_alarm_arn}"

  https_domain = "services.wellcomecollection.org"
}
