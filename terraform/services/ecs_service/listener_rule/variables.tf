variable "host_name" {
  description = "Hostname to be matched in the host condition"
  default     = ""
}

variable "listener_arn" {
  description = "ARN of listener for listener rule"
}

variable "alb_priority" {
  description = "ALB listener rule priority"
  default     = "100"
}

variable "target_group_arn" {
  description = "ARN of the target group for the listener rule"
}

variable "path_pattern" {
  description = "path pattern to match for listener rule"
  default     = "/*"
}
