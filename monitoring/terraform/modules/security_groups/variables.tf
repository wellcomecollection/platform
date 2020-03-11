variable "custom_security_groups" {
  type    = "list"
  default = []
}

variable "vpc_id" {}
variable "name" {}

variable "controlled_access_cidr_ingress" {
  type    = "list"
  default = []
}

variable "controlled_access_security_groups" {
  type    = "list"
  default = []
}
