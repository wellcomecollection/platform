variable "api_id" {}
variable "cognito_id" {}

variable "auth_scopes" {
  type = "list"
}

variable "root_resource_id" {}
variable "path_part" {}
variable "connection_id" {}

variable "hostname" {
  default = "api.wellcomecollection.org"
}

variable "forward_port" {}
variable "forward_path" {}
