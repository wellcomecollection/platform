module "elasticdump" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v7.0.1"
  name   = "elasticdump"

  source_queue_name  = "${module.elasticdump_queue.name}"
  source_queue_arn   = "${module.elasticdump_queue.arn}"
  ecr_repository_url = "${module.ecr_repository_elasticdump.repository_url}"
  release_id         = "${var.release_ids["elasticdump"]}"

  env_vars = {
    AWS_DEFAULT_REGION = "${var.aws_region}"

    TOPIC_ARN = "${module.snapshot_convertor_topic.arn}"

    sqs_queue_url = "${module.elasticdump_queue.id}"
    key_prefix    = "elasticdump/"
    upload_bucket = "${aws_s3_bucket.private_data.id}"
    es_username   = "${local.es_username}"
    es_password   = "${local.es_password}"
    es_hostname   = "${local.es_name}.${local.es_region}.aws.found.io"
    es_port       = "${local.es_port}"
    es_index      = "${local.es_index}"
  }

  env_vars_length = 9

  memory = 1024
  cpu    = 512

  cluster_name = "${module.data_api_cluster.cluster_name}"
  vpc_id       = "${module.vpc_data_api.vpc_id}"

  alb_cloudwatch_id          = "${module.data_api_cluster.alb_cloudwatch_id}"
  alb_listener_https_arn     = "${module.data_api_cluster.alb_listener_https_arn}"
  alb_listener_http_arn      = "${module.data_api_cluster.alb_listener_http_arn}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"

  enable_alb_alarm = false

  max_capacity = 1
}
