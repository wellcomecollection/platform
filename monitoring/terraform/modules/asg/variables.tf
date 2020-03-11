variable "asg_name" {
  description = "Name of the ASG to create"
}

variable "subnet_list" {
  type = list(string)
}

variable "launch_config_name" {}
