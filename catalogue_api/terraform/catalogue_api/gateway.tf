module "resource" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/resource/no_auth?ref=cfbc6c413003f953768e2ff97f47fad3f1f68ea5"

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
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/gateway?ref=f26492815b0fe25742b0d01652b8009e5db2fcbf"

  name = "Catalogue API"
}

resource "aws_api_gateway_deployment" "deployment" {
  rest_api_id = "${module.gateway.id}"
  stage_name  = "prod"
}

