resource "aws_iam_role" "static_resource_role" {
  name               = "${var.namespace}_static_resource_role"
  assume_role_policy = "${data.aws_iam_policy_document.api_gateway_assume_role.json}"
}

data "aws_iam_policy_document" "api_gateway_assume_role" {
  statement {
    actions = [
      "sts:AssumeRole",
    ]

    principals {
      type        = "Service"
      identifiers = ["apigateway.amazonaws.com"]
    }
  }
}
