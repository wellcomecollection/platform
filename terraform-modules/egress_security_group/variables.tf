variable "name" {}
variable "description" {}

variable "vpc_id" {}
variable "private_route_table_ids" {
  type = "list"
}
