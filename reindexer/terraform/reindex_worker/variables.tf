variable "aws_region" {
  default = "eu-west-1"
}

variable "account_id" {}

variable "reindex_worker_container_image" {}

variable "service_egress_security_group_id" {}

variable "ecs_cluster_name" {}
variable "ecs_cluster_id" {}

variable "namespace_id" {}

variable "reindexer_jobs" {
  type = "list"
}

variable "reindexer_job_config_json" {}

variable "scale_up_period_in_minutes" {
  default = 1
}

variable "scale_down_period_in_minutes" {
  default = 10
}
