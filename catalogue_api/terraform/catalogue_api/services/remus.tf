module "remus" {
  source = "service"

  namespace = "${var.namespace}-remus"

  container_image = "${var.remus_container_image}"
  container_port  = "${var.container_port}"

  namespace_id = "${var.namespace_id}"
  cluster_name = "${var.cluster_name}"
  vpc_id       = "${var.vpc_id}"

  security_group_ids = [
    "${aws_security_group.service_lb_ingress_security_group.id}",
    "${aws_security_group.interservice_security_group.id}",
  ]

  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"

  subnets = ["${var.subnets}"]

  es_config = "${var.remus_es_config}"

  nginx_container_image = "${var.nginx_container_image}"
  nginx_container_port  = "${var.nginx_container_port}"

  lb_arn        = "${var.nlb_arn}"
  listener_port = "${var.remus_listener_port}"

  task_desired_count = "${var.remus_task_number}"
}
