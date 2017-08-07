variable "name" {
  description = "Name of the alarm"
}

variable "lb_dimension" {
  description = "LoadBalancer ARN Suffix"
}

variable "tg_dimension" {
  description = "TargetGroup ARN Suffix"
}

variable "topic_arn" {
  description = "SNS Topic to publish alarm state changes"
}

variable "enable_alarm" {
  default = true
}

variable "metric" {}
