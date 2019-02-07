variable "remus_es_config" {
  type = "map"
}

variable "romulus_es_config" {
  type = "map"
}

variable "v1_amber_es_config" {
  type = "map"
}

variable "remus_task_number" {}
variable "romulus_task_number" {}
variable "v1_amber_task_number" {}

variable "subnets" {
  type = "list"
}

variable "vpc_id" {}

variable "remus_container_image" {}
variable "romulus_container_image" {}
variable "v1_amber_container_image" {}
variable "nginx_container_image" {}

variable "container_port" {}

variable "nginx_container_port" {
  default = "9000"
}

variable "cluster_name" {}

variable "namespace" {}

variable "production_api" {}
variable "stage_api" {}

variable "alarm_topic_arn" {}
