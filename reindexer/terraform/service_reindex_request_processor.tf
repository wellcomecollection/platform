module "reindex_request_processor" {
  source       = "git::https://github.com/wellcometrust/terraform-modules.git//ecs/modules/service/prebuilt/sqs_scaling?ref=v11.4.1"
  service_name = "reindex_request_processor"

  task_desired_count = "0"
  source_queue_name  = "${module.reindex_requests_queue.name}"
  source_queue_arn   = "${module.reindex_requests_queue.arn}"

  container_image = "${local.reindex_request_processor_container_image}"
  cpu             = 512
  memory          = 2048

  env_vars = {
    dynamo_table_name         = "${local.vhs_sourcedata_table_name}"
    reindex_requests_queue_id = "${module.reindex_requests_queue.id}"
    metrics_namespace         = "reindex_request_processor"
  }

  env_vars_length = 3

  ecs_cluster_name   = "${aws_ecs_cluster.cluster.name}"
  ecs_cluster_id     = "${aws_ecs_cluster.cluster.id}"
  vpc_id             = "${local.vpc_id}"
  security_group_ids = ["${aws_security_group.service_egress_security_group.id}"]

  aws_region = "${var.aws_region}"
  subnets    = ["${local.private_subnets}"]

  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"

  launch_type = "FARGATE"

  max_capacity = 15
}

resource "aws_iam_role_policy" "reindex_processor_task_cloudwatch_metric" {
  role   = "${module.reindex_request_processor.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

resource "aws_iam_role_policy" "reindex_processor_allow_table_access" {
  role   = "${module.reindex_request_processor.task_role_name}"
  policy = "${local.vhs_sourcedata_full_access_policy}"
}
