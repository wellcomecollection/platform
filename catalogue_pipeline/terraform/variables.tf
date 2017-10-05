variable "release_ids" {
  description = "Release tags for platform apps"
  type        = "map"
}

variable "key_name" {
  description = "Name of AWS key pair"
}

variable "admin_cidr_ingress" {
  description = "CIDR to allow tcp/22 ingress to EC2 instance"
}

variable "infra_bucket" {
  description = "S3 bucket storing our configuration"
  default     = "platform-infra"
}

variable "build_env" {
  description = "Build environment (prod, dev, stage, ...)"
  default     = "prod"
}

variable "aws_region" {
  description = "The AWS region to create things in."
  default     = "eu-west-1"
}

variable "rds_username" {
  description = "Username for the RDS instance"
}

variable "rds_password" {
  description = "Password7 for the RDS database"
}

variable "es_config_ingestor" {
  description = "ElasticCloud config for the ingestor"
  type        = "map"
  default     = {}
}

variable "ec2_terminating_topic_arn" {}
variable "ec2_instance_terminating_for_too_long_alarm_arn" {}
variable "ec2_terminating_topic_publish_policy" {}
variable "alb_server_error_alarm_arn" {}
variable "alb_client_error_alarm_arn" {}
variable "dlq_alarm_arn" {}
variable "terminal_failure_alarm_arn" {}
