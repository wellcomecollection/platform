module "bags" {
  source = "../../../modules/service/api"

  namespace = "${var.namespace}-bags"

  container_image = "${var.bags_container_image}"
  container_port  = "${var.bags_container_port}"

  namespace_id  = "${var.namespace_id}"
  namespace_tld = "${var.namespace_tld}"

  cluster_id   = "${var.cluster_id}"
  cluster_name = "${var.cluster_name}"

  vpc_id = "${var.vpc_id}"

  security_group_ids = [
    "${aws_security_group.service_egress_security_group.id}",
    "${aws_security_group.service_lb_ingress_security_group.id}",
    "${var.interservice_security_group_id}",
  ]

  subnets = ["${var.subnets}"]

  nginx_container_port  = "${var.bags_nginx_container_port}"
  nginx_container_image = "${var.bags_nginx_container_image}"

  env_vars = "${var.bags_env_vars}"

  env_vars_length = "${var.bags_env_vars_length}"

  lb_arn        = "${var.nlb_arn}"
  listener_port = "${var.bags_listener_port}"
}
