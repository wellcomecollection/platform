variable "name" {
  description = "Name of the alarm"
}

variable "metric_name" {
  description = "Cloudwatch metric name"
}

variable "namespace" {
  description = "Cloudwatch namspace"
}

variable "alarm_action_arn" {
  description = "ARN of Cloudwatch action to take e.g. SNS topic"
}
