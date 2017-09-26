variable "asg_name" {
  description = "Name of the ASG to create"
}

variable "asg_min" {
  description = "Minimum number of instances"
}

variable "asg_desired" {
  description = "Desired number of instances"
}

variable "asg_max" {
  description = "Max number of instances"
}

variable "subnet_list" {
  type = "list"
}

variable "sns_topic_arn" {
  description = "ARN of the topic where to push notifications when an EC2 instance is set to terminating state"
}

variable "publish_to_sns_policy" {
  description = "Policy document to give permission to publish to the sns topic"
}

variable "alarm_topic_arn" {
  description = "ARN of the topic where to send notification for DLQs not being empty"
}

variable "launch_config_name" {}
