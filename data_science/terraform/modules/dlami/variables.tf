variable "name" {
  description = "Name of the ASG to create"
}

variable "subnet_list" {
  type = "list"
}

variable "key_name" {
  description = "SSH key pair name for instance sign-in"
}

variable "vpc_id" {
  description = "VPC for EC2 autoscaling group security group"
}

variable "controlled_access_cidr_ingress" {
  type        = "list"
  default     = []
  description = "CIDR for SSH access to EC2 instances"
}

variable "custom_security_groups" {
  type    = "list"
  default = []
}

# DLAMI specific variables

variable "instance_type" {
  default     = "t2.large"
  description = "AWS instance type"
}

variable "image_id" {
  description = "ID of the AMI to use on the instances"

  # Ubuntu DLAMI
  default = "ami-0bc19972"
}

variable "enabled" {
  default = false
}

variable "default_environment" {
  description = "Python environment to install modules in"
}

variable "spot_price" {
  description = "Maximum spot price to use for the instances"
}

variable "hashed_password" {
  default = "sha1:5310f21e370d:a4d66e725c179218638c21c03d83933aa066db2d"
}

variable "bucket_name" {
  description = "Bucket for storing Jupyter notebooks with s3contents plugin"
}

variable "efs_mount_id" {}
