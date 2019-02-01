module "transformer" {
  source = "../service"

  service_name = "${var.namespace}_transformer"

  container_image = "${var.container_image}"

  service_egress_security_group_id = "${var.service_egress_security_group_id}"

  cluster_name = "${var.cluster_name}"
  cluster_id   = "${var.cluster_id}"

  namespace_id = "${var.namespace_id}"

  vpc_id  = "${var.vpc_id}"
  subnets = "${var.subnets}"

  aws_region = "${var.aws_region}"

  env_vars = {
    sns_arn              = "${var.transformed_works_topic_arn}"
    transformer_queue_id = "${module.transformer_queue.id}"
    metrics_namespace    = "${var.namespace}_transformer"
    message_bucket_name  = "${var.message_bucket_name}"
  }

  env_vars_length = 4

  secret_env_vars        = {}
  secret_env_vars_length = "0"
}
