variable "app_name" {
  description = "Name of the app to be configured"
}

variable "infra_bucket" {
  description = "Name of the AWS Infra bucket"
}

variable "template_vars" {
  description = "Variables for the template file"
  type        = "map"
}
