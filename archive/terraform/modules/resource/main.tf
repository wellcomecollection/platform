module "auth_resource" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/resource?ref=v16.1.8"

  api_id = "${var.api_id}"

  authorization = "COGNITO_USER_POOLS"
  authorizer_id = "${var.cognito_id}"
  auth_scopes   = ["${var.auth_scopes}"]

  parent_id = "${var.root_resource_id}"
  path_part = "${var.path_part}"
}

module "auth_resource_integration" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/integration/proxy?ref=v16.1.8"

  api_id        = "${var.api_id}"
  resource_id   = "${module.auth_resource.resource_id}"
  connection_id = "${var.connection_id}"

  hostname    = "${var.hostname}"
  http_method = "${module.auth_resource.http_method}"

  forward_port = "${var.forward_port}"
  forward_path = "${var.forward_path}"
}

module "auth_subresource" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/resource?ref=v16.1.8"

  api_id = "${var.api_id}"

  authorization = "COGNITO_USER_POOLS"
  authorizer_id = "${var.cognito_id}"
  auth_scopes   = ["${var.auth_scopes}"]

  parent_id = "${module.auth_resource.resource_id}"
  path_part = "{proxy+}"

  request_parameters = {
    "method.request.path.proxy" = true
  }
}

module "auth_subresource_integration" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/integration/proxy?ref=v16.1.8"

  api_id        = "${var.api_id}"
  resource_id   = "${module.auth_subresource.resource_id}"
  connection_id = "${var.connection_id}"

  hostname    = "www.example.com"
  http_method = "${module.auth_subresource.http_method}"

  forward_port = "${var.forward_port}"
  forward_path = "${var.forward_path}/{proxy}"

  request_parameters = {
    integration.request.path.proxy = "method.request.path.proxy"
  }
}
