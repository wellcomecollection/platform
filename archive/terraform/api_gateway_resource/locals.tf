locals {
  integration_uri     = "http://${var.hostname}:${var.load_balancer_port}/${var.forward_path}"
  authorization_scope = "${var.cognito_storage_api_identifier}/${var.resource_name}"
}
