module "goobi_reader_service" {
  source       = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/service/prebuilt/sqs_scaling?ref=v11.4.1"
  service_name = "${var.service_name}"

  task_desired_count = "0"

  container_image = "${var.container_image}"

  source_queue_name = "${module.goobi_mets_queue.name}"
  source_queue_arn  = "${module.goobi_mets_queue.arn}"

  env_vars = {
    goobi_mets_queue_url = "${module.goobi_mets_queue.id}"
    metrics_namespace    = "${var.service_name}"
    vhs_goobi_tablename  = "${var.vhs_goobi_tablename}"
    vhs_goobi_bucketname = "${var.vhs_goobi_bucketname}"
  }

  env_vars_length = 4

  cpu    = 256
  memory = 1024

  ecs_cluster_name = "${aws_ecs_cluster.cluster.name}"

  aws_region = "${var.aws_region}"
  vpc_id     = "${var.vpc_id}"

  max_capacity = 15

  ecs_cluster_id = "${aws_ecs_cluster.cluster.id}"

  subnets = "${var.subnets}"

  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"

  launch_type = "FARGATE"
}
