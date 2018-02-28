data "aws_s3_bucket" "dashboard" {
  bucket = "${var.dashboard_bucket}"
}

data "aws_iam_policy_document" "s3_put_dashboard_status" {
  statement {
    actions = [
      "s3:PutObject",
      "s3:GetObjectACL",
      "s3:PutObjectACL",
    ]

    resources = [
      "${data.aws_s3_bucket.dashboard.arn}/data/*",
    ]
  }
}

data "aws_iam_policy_document" "describe_services" {
  statement {
    actions = [
      "ecs:DescribeServices",
      "ecs:DescribeClusters",
      "ecs:DescribeTaskDefinition",
      "ecs:ListClusters",
      "ecs:ListServices",
    ]

    resources = [
      "*",
    ]
  }
}

data "aws_iam_policy_document" "assume_roles" {
  statement {
    actions = ["sts:AssumeRole"]

    resources = [
      "arn:aws:iam::130871440101:role/platform-team-assume-role",
      "arn:aws:iam::299497370133:role/platform-team-assume-role",
    ]
  }
}
