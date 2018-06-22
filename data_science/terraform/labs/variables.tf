variable "namespace" {}

variable "subnets" {
  type    = "list"
  default = []
}
variable "vpc_cidr_block" {}
variable "vpc_id" {}
