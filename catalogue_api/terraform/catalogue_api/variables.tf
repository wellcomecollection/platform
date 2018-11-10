variable "remus_es_config" {
  type = "map"
}

variable "romulus_es_config" {
  type = "map"
}

variable "es_cluster_credentials" {
  type = "map"
}

variable "subnets" {
  type = "list"
}

variable "vpc_id" {}

variable "remus_container_image" {}
variable "romulus_container_image" {}

variable "container_port" {}

variable "cluster_name" {}

variable "namespace" {}
variable "namespace_id" {}
