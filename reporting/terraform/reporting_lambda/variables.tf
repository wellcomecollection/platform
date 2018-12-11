variable "timeout" {
  default = 25
}

variable "name" {}
variable "description" {}

variable "environment_variables" {
  type = "map"
}

variable "topic_arns" {
  type        = "list"
  description = "Topic arn for the SNS topic to subscribe the queue to"
}

variable "topic_count" {
  default = 1
}

variable "error_alarm_topic_arn" {}

variable "log_retention_in_days" {
  default = 7
}

variable "vhs_read_policy" {}
