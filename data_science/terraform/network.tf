module "network" {
  source     = "git::https://github.com/wellcometrust/terraform.git//network?ref=v11.0.0"
  name       = "${var.namespace}"
  cidr_block = "${var.vpc_cidr_block}"
  az_count   = "2"
}

resource "aws_vpc_endpoint" "s3" {
  vpc_id       = "${module.network.vpc_id}"
  service_name = "com.amazonaws.${var.aws_region}.s3"
}

data "aws_vpc_endpoint_service" "dynamodb" {
  service = "dynamodb"
}

resource "aws_vpc_endpoint" "dynamodb" {
  vpc_id       = "${module.network.vpc_id}"
  service_name = "${data.aws_vpc_endpoint_service.dynamodb.service_name}"
}
