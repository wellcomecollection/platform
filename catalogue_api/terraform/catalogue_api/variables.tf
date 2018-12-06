variable "remus_es_config" {
  type = "map"
}

variable "romulus_es_config" {
  type = "map"
}

variable "es_cluster_credentials" {
  type = "map"
}

variable "es_cluster_credentials_v6" {
  type = "map"
}

variable "subnets" {
  type = "list"
}

variable "vpc_id" {}

variable "remus_container_image" {}
variable "romulus_container_image" {}
variable "nginx_container_image" {}

variable "container_port" {}

variable "nginx_container_port" {
  default = "9000"
}

variable "cluster_name" {}

variable "namespace" {}

variable "production_api" {}
