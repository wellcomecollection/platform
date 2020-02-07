variable "name" {}
variable "federated_principal" {}
variable "aws_principal" {}

variable "assumable_role_arns" {
  type = list(string)
}
