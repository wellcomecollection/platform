module "service" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/prebuilt/rest?ref=v14.2.0"

  vpc_id       = "${var.vpc_id}"
  subnets      = ["${var.subnets}"]
  cluster_name = "${var.cluster_name}"
  service_name = "${var.namespace}"
  namespace_id = "${var.namespace_id}"

  container_image = "${var.container_image}"
  container_port  = "${var.container_port}"

  env_vars = "${var.env_vars}"

  env_vars_length = "${var.env_vars_length}"

  security_group_ids               = ["${var.security_group_ids}"]
  service_egress_security_group_id = "${var.service_egress_security_group_id}"

  target_group_protocol = "TCP"
}

module "nginx" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/prebuilt/rest?ref=v14.2.0"

  vpc_id       = "${var.vpc_id}"
  subnets      = ["${var.subnets}"]
  cluster_name = "${var.cluster_name}"
  service_name = "${var.namespace}-nginx"
  namespace_id = "${var.namespace_id}"

  container_image = "${var.nginx_container_image}"
  container_port  = "${var.nginx_container_port}"

  env_vars = {
    APP_HOST = "${var.namespace}.${var.namespace_tld}"
    APP_PORT = "${var.container_port}"
  }

  env_vars_length = "2"

  security_group_ids               = ["${var.security_group_ids}"]
  service_egress_security_group_id = "${var.service_egress_security_group_id}"

  target_group_protocol = "TCP"
}
