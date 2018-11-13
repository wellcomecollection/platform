variable "es_config" {
  type = "map"
}

variable "subnets" {
  type = "list"
}

variable "cluster_name" {}

variable "namespace" {}
variable "namespace_id" {}

data "template_file" "es_cluster_host" {
  template = "$${name}.$${region}.aws.found.io"

  vars {
    name   = "${var.es_cluster_credentials["name"]}"
    region = "${var.es_cluster_credentials["region"]}"
  }
}

variable "es_cluster_credentials" {
  type = "map"
}

variable "vpc_id" {}

variable "container_image" {}
variable "container_port" {}

variable "nginx_container_image" {}
variable "nginx_container_port" {}

variable "security_group_ids" {
  type = "list"
}

variable "service_egress_security_group_id" {}
