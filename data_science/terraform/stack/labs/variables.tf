variable "namespace" {}

variable "public_subnets" {
  type    = "list"
  default = []
}

variable "private_subnets" {
  type    = "list"
  default = []
}

variable "vpc_id" {}
