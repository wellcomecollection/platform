module "service" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/prebuilt/scaling?ref=v19.7.2"

  service_name = "${var.service_name}"

  container_image = "${var.container_image}"

  cluster_id   = "${var.cluster_id}"
  cluster_name = "${var.cluster_name}"

  subnets    = "${var.subnets}"
  aws_region = "${var.aws_region}"

  namespace_id = "${var.namespace_id}"

  cpu    = "256"
  memory = "512"

  security_group_ids               = ["${var.security_group_ids}"]
  service_egress_security_group_id = ""

  env_vars        = "${var.env_vars}"
  env_vars_length = "${var.env_vars_length}"

  secret_env_vars        = "${var.secret_env_vars}"
  secret_env_vars_length = "${var.secret_env_vars_length}"

  metric_namespace = "AWS/SQS"
  high_metric_name = "foo"
  low_metric_name  = "bar"
}
