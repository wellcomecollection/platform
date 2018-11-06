
variable "es_config" {
  type = "map"
}
variable "es_cluster_credentials" {
  type = "map"
}
variable "subnets" {
  type = "list"
}

variable "vpc_id" {}
variable "container_image" {}

variable "cluster_name" {}

variable "namespace" {}
variable "namespace_id" {}

variable "external_path" {
  default = "catalogue"
}

variable "internal_port" {
  default = "8888"
}

variable "internal_path" {
  default = ""
}