variable "storage_api_id" {}

variable "storage_api_root_resource_id" {}

variable "authorizer_id" {}

variable "cognito_storage_api_identifier" {}
variable "vpc_link_id" {}

variable "load_balancer_port" {}

variable "hostname" {
  default = "api.wellcomecollection.org"
}

variable "forward_path" {}

variable "resource_name" {}
