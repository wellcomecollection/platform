module "catalogue_api" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/prebuilt/rest/container_with_sidecar?ref=v16.1.0"

  vpc_id       = "${var.vpc_id}"
  subnets      = ["${var.subnets}"]
  cluster_name = "${var.cluster_name}"
  service_name = "${var.namespace}"
  namespace_id = "${var.namespace_id}"

  security_group_ids               = ["${var.security_group_ids}"]
  service_egress_security_group_id = "${var.service_egress_security_group_id}"

  target_group_protocol = "TCP"

  app_container_image = "${var.container_image}"
  app_container_port  = "${var.container_port}"

  app_env_vars = {
    api_host    = "api.wellcomecollection.org"
    es_host     = "${data.template_file.es_cluster_host.rendered}"
    es_port     = "${var.es_cluster_credentials["port"]}"
    es_username = "${var.es_cluster_credentials["username"]}"
    es_password = "${var.es_cluster_credentials["password"]}"
    es_protocol = "${var.es_cluster_credentials["protocol"]}"
    es_index_v1 = "${var.es_config["index_v1"]}"
    es_index_v2 = "${var.es_config["index_v2"]}"
    es_doc_type = "${var.es_config["doc_type"]}"
  }

  app_env_vars_length = "9"

  sidecar_container_image = "${var.nginx_container_image}"
  sidecar_container_port  = "${var.nginx_container_port}"

  sidecar_env_vars = {
    APP_HOST = "localhost"
    APP_PORT = "${var.container_port}"
  }

  sidecar_env_vars_length = "2"

  lb_arn        = "${var.lb_arn}"
  listener_port = "${var.listener_port}"

  target_container = "sidecar"
}