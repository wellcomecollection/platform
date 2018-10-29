resource "aws_api_gateway_vpc_link" "progress" {
  name        = "${var.resource_name}_vpc_link"
  target_arns = ["${var.load_balancer_arn}"]
}