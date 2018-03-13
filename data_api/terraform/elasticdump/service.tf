module "elasticdump" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v7.0.1"
  name   = "elasticdump"

  source_queue_name  = "${module.elasticdump_queue.name}"
  source_queue_arn   = "${module.elasticdump_queue.arn}"
  ecr_repository_url = "${module.ecr_repository.repository_url}"
  release_id         = "${var.release_ids["elasticdump"]}"

  env_vars = {
    sqs_queue_url = "${module.elasticdump_queue.url}"
    es_username
    es_password
    es_name
    es_region
    es_port
    es_index
  }

  memory = 1024
  cpu    = 512

  env_vars_length = 7

  cluster_name = "${var.cluster_name}"
  vpc_id       = "${var.vpc_id}"

  alb_cloudwatch_id          = "${var.alb_cloudwatch_id}"
  alb_listener_https_arn     = "${var.alb_listener_https_arn}"
  alb_listener_http_arn      = "${var.alb_listener_http_arn}"
  alb_server_error_alarm_arn = "${var.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${var.alb_client_error_alarm_arn}"

  enable_alb_alarm = false

  max_capacity = 1
}
