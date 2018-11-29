resource "aws_vpc_endpoint" "s3" {
  vpc_id       = "${local.vpc_id}"
  service_name = "com.amazonaws.${var.aws_region}.s3"
}

data "aws_vpc_endpoint_service" "dynamodb" {
  service = "dynamodb"
}

resource "aws_vpc_endpoint" "dynamodb" {
  vpc_id       = "${local.vpc_id}"
  service_name = "${data.aws_vpc_endpoint_service.dynamodb.service_name}"
}
