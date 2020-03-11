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

variable "launch_config_name" {}
