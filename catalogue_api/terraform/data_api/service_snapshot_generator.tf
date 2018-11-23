data "template_file" "es_cluster_host_snapshot" {
  template = "$${name}.$${region}.aws.found.io"

  vars {
    name   = "${var.es_cluster_credentials["name"]}"
    region = "${var.es_cluster_credentials["region"]}"
  }
}

module "snapshot_generator" {
  source             = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/service/prebuilt/sqs_scaling?ref=v11.4.0"
  service_name       = "snapshot_generator"
  task_desired_count = "0"

  source_queue_name = "${module.snapshot_generator_queue.name}"
  source_queue_arn  = "${module.snapshot_generator_queue.arn}"

  env_vars = {
    queue_url         = "${module.snapshot_generator_queue.id}"
    topic_arn         = "${module.snapshot_generation_complete_topic.arn}"
    es_host           = "${data.template_file.es_cluster_host_snapshot.rendered}"
    es_port           = "${var.es_cluster_credentials["port"]}"
    es_username       = "${var.es_cluster_credentials["username"]}"
    es_password       = "${var.es_cluster_credentials["password"]}"
    es_protocol       = "${var.es_cluster_credentials["protocol"]}"
    es_index_v1       = "${var.es_config_snapshot["index_v1"]}"
    es_index_v2       = "${var.es_config_snapshot["index_v2"]}"
    es_doc_type       = "${var.es_config_snapshot["doc_type"]}"
    metrics_namespace = "snapshot_generator"
  }

  env_vars_length = 11

  memory = 4096
  cpu    = 2048

  vpc_id = "${var.vpc_id}"

  max_capacity = 2

  scale_down_period_in_minutes = 30

  container_image = "${module.ecr_repository_snapshot_generator.repository_url}:${var.snapshot_generator_release_id}"

  ecs_cluster_id   = "${aws_ecs_cluster.cluster.id}"
  ecs_cluster_name = "${aws_ecs_cluster.cluster.name}"

  aws_region = "${var.aws_region}"
  vpc_id     = "${var.vpc_id}"
  subnets    = "${var.private_subnets}"

  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"

  launch_type = "FARGATE"
}
