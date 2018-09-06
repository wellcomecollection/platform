module "transformer" {
  source = "../service"

  service_egress_security_group_id = "${var.service_egress_security_group_id}"
  cluster_name                     = "${var.cluster_name}"
  namespace_id                     = "${var.namespace_id}"
  subnets                          = "${var.subnets}"
  vpc_id                           = "${var.vpc_id}"
  service_name                     = "${var.namespace}_${var.source_name}_transformer"
  aws_region                       = "${var.aws_region}"

  env_vars = {
    sns_arn              = "${var.transformed_works_topic_arn}"
    transformer_queue_id = "${module.transformer_queue.id}"
    metrics_namespace    = "${var.namespace}_${var.source_name}_transformer"
    message_bucket_name  = "${var.messages_bucket}"
  }

  env_vars_length = 4

  container_image   = "${var.transformer_container_image}"
  source_queue_name = "${module.transformer_queue.name}"
  source_queue_arn  = "${module.transformer_queue.arn}"
}
