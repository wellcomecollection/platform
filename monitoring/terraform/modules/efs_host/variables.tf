variable "cluster_name" {}

variable "asg_name" {
  description = "Name of the ASG"
}

variable "instance_type" {
  default     = "t2.small"
  description = "AWS instance type"
}

variable "subnets" {
  type = list(string)
}

variable "vpc_id" {}
variable "key_name" {}

variable "image_id" {
  default = "ami-c91624b0"
}

variable "custom_security_groups" {
  type    = list(string)
  default = []
}

variable "efs_fs_id" {}
variable "region" {}

variable "efs_host_path" {
  default = "/efs"
}
