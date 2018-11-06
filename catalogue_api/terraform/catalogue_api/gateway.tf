module "resource" {
  source = "../../../../terraform-modules/api_gateway/modules/resource/no_auth"

  namespace     = "${var.namespace}"
  resource_name = "${var.external_path}"

  api_id               = "${module.gateway.id}"
  api_root_resource_id = "${module.gateway.root_resource_id}"

  proxied_hostname = "api.wellcomecollection.org"

  forward_port = "${var.internal_port}"
  forward_path = "${var.internal_path}"

  target_arns = ["${module.nlb.arn}"]
}

module "gateway" {
  source = "../../../../modules/gateway"

  name = "Catalogue API"
}

resource "aws_api_gateway_deployment" "deployment" {
  rest_api_id = "${module.gateway.id}"
  stage_name  = "prod"
}