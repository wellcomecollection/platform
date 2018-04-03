module "snapshot_convertor_job_generator" {
  source = "snapshot_convertor_job_generator"

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
  infra_bucket           = "${var.infra_bucket}"
}

data "aws_sns_topic" "snapshot_convertor_topic" {
  name = "${module.snapshot_convertor_job_generator.topic_name}"
}

module "snapshot_convertor" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=v8.0.0"
  name   = "snapshot_convertor"

  source_queue_name = "${data.aws_sns_topic.snapshot_convertor_topic.name}"
  source_queue_arn  = "${data.aws_sns_topic.snapshot_convertor_topic.arn}"

  ecr_repository_url = "${module.ecr_repository_snapshot_convertor.repository_url}"
  release_id         = "${var.release_ids["snapshot_convertor"]}"

  env_vars = {
    source_bucket_name = "${aws_s3_bucket.private_data.id}"
    target_bucket_name = "${aws_s3_bucket.public_data.id}"

    queue_url = "${module.snapshot_convertor_queue.id}"
    topic_arn = "${module.snapshot_conversion_complete_topic.arn}"
  }

  memory = 2048
  cpu    = 512

  env_vars_length = 4

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
