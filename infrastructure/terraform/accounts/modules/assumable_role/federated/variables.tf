variable "name" {}

variable "federated_principal" {}

variable "aws_principal" {}

variable "max_session_duration_in_seconds" {
  # Default is one hour
  default = "3600"
}
