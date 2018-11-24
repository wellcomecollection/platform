module "rest_service_with_sidecar" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/prebuilt/rest/container_with_sidecar?ref=c0e6476a6502ed1bc8876e273ed4ab09be281958"

  vpc_id  = "${var.vpc_id}"
  subnets = ["${var.subnets}"]

  cluster_name = "${var.cluster_name}"
  cluster_id   = "${var.cluster_id}"

  service_name = "${var.namespace}"
  namespace_id = "${var.namespace_id}"

  app_container_image = "${var.container_image}"
  app_container_port  = "${var.container_port}"

  app_env_vars = "${var.env_vars}"

  app_env_vars_length = "${var.env_vars_length}"

  sidecar_container_image = "${var.nginx_container_image}"
  sidecar_container_port  = "${var.nginx_container_port}"

  sidecar_env_vars = {
    APP_HOST = "localhost"
    APP_PORT = "${var.container_port}"
  }

  sidecar_env_vars_length = "2"

  security_group_ids               = ["${var.security_group_ids}"]
  service_egress_security_group_id = "${var.service_egress_security_group_id}"

  lb_arn        = "${var.lb_arn}"
  listener_port = "${var.listener_port}"

  target_group_protocol = "TCP"

  target_container = "sidecar"
}
