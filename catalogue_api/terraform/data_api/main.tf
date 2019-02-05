module "snapshot_generator" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/prebuilt/scaling?ref=v19.7.2"

  service_name = "snapshot_generator"
  cluster_id   = "${aws_ecs_cluster.cluster.id}"
  cluster_name = "${aws_ecs_cluster.cluster.name}"

  subnets = "${var.private_subnets}"

  namespace_id    = "${local.service_discovery_namespace}"
  container_image = "${var.snapshot_generator_release_uri}"

  env_vars = {
    queue_url = "${module.snapshot_generator_queue.id}"
    topic_arn = "${module.snapshot_complete_topic.arn}"

    es_index_v1 = "${var.es_config_snapshot["index_v1"]}"
    es_index_v2 = "${var.es_config_snapshot["index_v2"]}"

    metric_namespace = "snapshot_generator"
  }

  env_vars_length = "5"

  secret_env_vars = {
    es_host     = "catalogue/api/es_host"
    es_port     = "catalogue/api/es_port"
    es_protocol = "catalogue/api/es_protocol"
    es_username = "catalogue/api/es_username"
    es_password = "catalogue/api/es_password"
  }

  secret_env_vars_length = "5"

  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"

  metric_namespace = "AWS/SQS"

  high_metric_name = "foo"
  low_metric_name  = "bar"
}

module "snapshot_scheduler" {
  source = "snapshot_scheduler"

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
  infra_bucket           = "${var.infra_bucket}"

  public_bucket_name = "${aws_s3_bucket.public_data.id}"

  public_object_key_v1 = "catalogue/v1/works.json.gz"
  public_object_key_v2 = "catalogue/v2/works.json.gz"
}
