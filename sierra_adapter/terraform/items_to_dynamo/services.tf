data "aws_ecs_cluster" "cluster" {
  cluster_name = "${var.cluster_name}"
}

module "sierra_to_dynamo_service" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v7.0.1"
  name   = "sierra_items_to_dynamo"

  source_queue_name  = "${module.demultiplexer_queue.name}"
  source_queue_arn   = "${module.demultiplexer_queue.arn}"
  ecr_repository_url = "${module.ecr_repository_sierra_to_dynamo.repository_url}"
  release_id         = "${var.release_id}"

  env_vars = {
    demultiplexer_queue_url = "${module.demultiplexer_queue.id}"
    metrics_namespace       = "sierra_items_to_dynamo"

    dynamo_table_name = "${aws_dynamodb_table.sierra_table.id}"
  }

  env_vars_length = 3

  cpu    = 256
  memory = 1024

  cluster_name = "${var.cluster_name}"
  vpc_id       = "${var.vpc_id}"

  alb_cloudwatch_id          = "${var.alb_cloudwatch_id}"
  alb_listener_https_arn     = "${var.alb_listener_https_arn}"
  alb_listener_http_arn      = "${var.alb_listener_http_arn}"
  alb_server_error_alarm_arn = "${var.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${var.alb_client_error_alarm_arn}"

  enable_alb_alarm = false

  max_capacity = 50
}
