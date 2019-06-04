variable "name" {}
variable "instance_type" {}

variable "hashed_password" {
  default = "sha1:5310f21e370d:a4d66e725c179218638c21c03d83933aa066db2d"
}

variable "bucket_name" {
  description = "Bucket for storing Jupyter notebooks with s3contents plugin"
}

variable "default_environment" {
  description = "Python environment to install modules in"
}

variable "vpc_id" {}
variable "custom_security_groups" {
  type        = "list"
  default     = []
}
variable "controlled_access_cidr_ingress" {
  type        = "list"
  default     = []
  description = "CIDR for SSH access to EC2 instances"
}
variable "key_name" {}
variable "ebs_volume_size" {
  default = 250
}

variable "efs_mount_id" {}


