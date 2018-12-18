module "service" {
  source       = "../scaling_service"
  service_name = "reindex_worker"

  task_desired_count = "0"
  source_queue_name  = "${module.reindex_worker_queue.name}"
  source_queue_arn   = "${module.reindex_worker_queue.arn}"

  container_image    = "${var.reindex_worker_container_image}"
  security_group_ids = ["${var.service_egress_security_group_id}"]

  cpu    = 1024
  memory = 1024

  env_vars = {
    reindex_jobs_queue_id     = "${module.reindex_worker_queue.id}"
    metrics_namespace         = "reindex_worker"
    reindexer_job_config_json = "${var.reindexer_job_config_json}"
  }

  env_vars_length = 3

  ecs_cluster_name = "${var.ecs_cluster_name}"
  ecs_cluster_id   = "${var.ecs_cluster_id}"
  vpc_id           = "${local.vpc_id}"

  aws_region = "${var.aws_region}"
  subnets    = ["${local.private_subnets}"]

  namespace_id = "${var.namespace_id}"

  launch_type = "FARGATE"

  max_capacity = 7
}
