module "goobi_adapter" {
  source                  = "./goobi_adapter"
  goobi_items_queue_name  = "goobi_items"
  goobi_items_bucket_name = "${aws_s3_bucket.goobi_adapter.id}"
  goobi_items_topic       = "${module.goobi_bucket_notifications_topic.name}"

  release_id              = "${var.release_ids["goobi_reader"]}"
  ecr_repository_url      = "${module.ecr_repository_goobi_reader.repository_url}"

  dlq_alarm_arn           = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"

  vpc_id                  = "${module.vpc_goobi_adapter.vpc_id}"
//  cluster_name            = "${aws_ecs_cluster.goobi_adapter_cluster.name}"
//  ecs_launch_type         = "FARGATE"
  account_id              = "${data.aws_caller_identity.current.account_id}"

  cluster_name = "${module.goobi_adapter_cluster.cluster_name}"

  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_cloudwatch_id          = "${module.goobi_adapter_cluster.alb_cloudwatch_id}"
  alb_listener_http_arn      = "${module.goobi_adapter_cluster.alb_listener_http_arn}"
  alb_listener_https_arn     = "${module.goobi_adapter_cluster.alb_listener_https_arn}"
}