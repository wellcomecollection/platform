variable "timeout" {
  default = 25
}

variable "name" {}
variable "description" {}

variable "environment_variables" {
  type = "map"
}

variable "trigger_topic_arn" {}
variable "error_alarm_topic_arn" {}

variable "log_retention_in_days" {
  default = 7
}
