locals {
  integration_uri = "http://${var.hostname}/${var.forward_path}"
  authorization_scope = "${var.cognito_storage_api_identifier}/${var.resource_name}"
}