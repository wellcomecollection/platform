module "reindex_request_creator" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//ecs/modules/service/prebuilt/sqs_scaling?ref=v11.4.1"
  service_name   = "reindex_request_creator"

  task_desired_count = "0"
  source_queue_name = "${module.reindexer_queue.name}"
  source_queue_arn  = "${module.reindexer_queue.arn}"

  container_image         = "${local.reindex_request_creator_container_image}"
  security_group_ids = ["${aws_security_group.service_egress_security_group.id}"]

  cpu    = 512
  memory = 2048

  env_vars = {
    dynamo_table_name          = "${local.vhs_table_name}"
    reindex_jobs_queue_id      = "${module.reindexer_queue.id}"
    reindex_requests_topic_arn = "${module.reindex_requests_topic.arn}"
    metrics_namespace          = "reindex_request_creator"
  }

  env_vars_length = 4

  ecs_cluster_name               = "${aws_ecs_cluster.cluster.name}"
  ecs_cluster_id   = "${aws_ecs_cluster.cluster.id}"
  vpc_id                     = "${local.vpc_id}"

  aws_region = "${var.aws_region}"
  subnets    =  ["${local.private_subnets}"]

  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"

  launch_type = "FARGATE"

  max_capacity = 5
}

resource "aws_iam_role_policy" "reindexer_reindexer_task_cloudwatch_metric" {
  role   = "${module.reindex_request_creator.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

resource "aws_iam_role_policy" "reindexer_allow_table_access" {
  role   = "${module.reindex_request_creator.task_role_name}"
  policy = "${local.vhs_full_access_policy}"
}

resource "aws_iam_role_policy" "reindex_creator_publish_requests" {
  role   = "${module.reindex_request_creator.task_role_name}"
  policy = "${module.reindex_requests_topic.publish_policy}"
}
