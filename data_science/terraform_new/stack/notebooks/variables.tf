variable "namespace" {}

variable "key_name" {}

variable "s3_bucket_name" {}
variable "s3_bucket_arn" {}

variable "hashed_password" {
  # The default password is 'password'
  # To generate a new password run the following Python code:
  #
  # from notebook.auth import passwd
  #   passwd()
  #
  default = "sha1:5310f21e370d:a4d66e725c179218638c21c03d83933aa066db2d"
}

variable "aws_region" {}
variable "vpc_cidr_block" {}
variable "vpc_id" {}

variable "subnets" {
  type    = "list"
  default = []
}

variable "controlled_access_cidr_ingress" {
  type = "list"
}

variable "efs_security_group_id" {}
variable "efs_id" {}