module "service" {
  source       = "git::https://github.com/wellcometrust/terraform-modules.git//ecs/modules/service/prebuilt/sqs_scaling?ref=v11.4.1"
  service_name = "reindex_worker-${var.namespace}"

  task_desired_count = "0"
  source_queue_name  = "${module.reindex_worker_queue.name}"
  source_queue_arn   = "${module.reindex_worker_queue.arn}"

  container_image    = "${var.reindex_worker_container_image}"
  security_group_ids = ["${var.service_egress_security_group_id}"]

  cpu    = 512
  memory = 2048

  env_vars = {
    reindex_jobs_queue_id     = "${module.reindex_worker_queue.id}"
    reindex_publish_topic_arn = "${module.hybrid_records_topic.arn}"
    metrics_namespace         = "reindex_worker-${var.namespace}"
    dynamo_table_name         = "${var.vhs_table_name}"

    # The reindex worker has to send lots of SNS notifications, and we've
    # seen issues where we exhaust the HTTP connection pool.  Turning down
    # the parallelism is an attempt to reduce the number of SNS messages in
    # flight, and avoid these errors.
    sqs_parallelism = 5
  }

  env_vars_length = 5

  ecs_cluster_name = "${var.ecs_cluster_name}"
  ecs_cluster_id   = "${var.ecs_cluster_id}"
  vpc_id           = "${local.vpc_id}"

  aws_region = "${var.aws_region}"
  subnets    = ["${local.private_subnets}"]

  namespace_id = "${var.namespace_id}"

  launch_type = "FARGATE"

  max_capacity = 5
}
