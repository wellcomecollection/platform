variable "asg_name" {
  description = "Name of the ASG to create"
}

variable "asg_min" {
  description = "Minimum number of instances"
  default     = "1"
}

variable "asg_desired" {
  description = "Desired number of instances"
  default     = "1"
}

variable "asg_max" {
  description = "Max number of instances"
  default     = "1"
}

variable "launch_config_name" {
  description = "Launch config for instances"
}

variable "subnet_list" {
  type = "list"
}
