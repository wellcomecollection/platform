variable "namespace" {}

variable "key_name" {}

variable "s3_bucket_name" {}
variable "s3_bucket_arn" {}

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
