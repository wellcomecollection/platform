variable "namespace" {}

variable "public_subnets" {
  type    = "list"
  default = []
}

variable "private_subnets" {
  type    = "list"
  default = []
}

variable "vpc_cidr_block" {}
variable "vpc_id" {}
