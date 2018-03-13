module "snapshot_convertor" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v7.0.1"
  name   = "snapshot_convertor"

  source_queue_name  = "${module.snapshot_convertor_topic.name}"
  source_queue_arn   = "${module.snapshot_convertor_topic.arn}"

  ecr_repository_url = "${module.ecr_repository_snapshot_convertor.repository_url}"
  release_id         = "${var.release_ids["id_minter"]}"

  env_vars = {
    source_bucket_name = "${aws_s3_bucket.private_data.id}"
    target_bucket_name = "${aws_s3_bucket.public_data.id}"

    queue_url   = "${module.snapshot_convertor_topic.id}"
    topic_arn   = "${module.snapshot_conversion_complete_topic.arn}"
  }

  memory = 2048
  cpu    = 512

  env_vars_length = 4

  cluster_name = "${module.data_api_cluster.cluster_name}"
  vpc_id       = "${module.vpc_data_api.vpc_id}"

  alb_priority = 105

  alb_cloudwatch_id          = "${module.data_api_cluster.alb_cloudwatch_id}"
  alb_listener_https_arn     = "${module.data_api_cluster.alb_listener_https_arn}"
  alb_listener_http_arn      = "${module.data_api_cluster.alb_listener_http_arn}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"

  enable_alb_alarm = false

  max_capacity = 15
}
