data "aws_iam_policy_document" "spot_fleet" {
  statement {
    actions = [
      "ec2:DescribeImages",
      "ec2:DescribeSubnets",
      "ec2:RequestSpotInstances",
      "ec2:TerminateInstances",
      "ec2:DescribeInstanceStatus",
      "iam:PassRole",
    ]

    resources = [
      "*",
    ]
  }
}

data "aws_iam_policy_document" "spot_fleet_assume_role" {
  statement {
    actions = [
      "sts:AssumeRole",
    ]

    principals {
      type        = "Service"
      identifiers = ["spotfleet.amazonaws.com"]
    }
  }
}

resource "aws_iam_role_policy" "spot_fleet" {
  name   = "${var.prefix}_AmazonEC2SpotFleetRole"
  role   = "${aws_iam_role.spot_fleet.name}"
  policy = "${data.aws_iam_policy_document.spot_fleet.json}"
}

resource "aws_iam_role" "spot_fleet" {
  name               = "${var.prefix}_AmazonEC2SpotFleetRole"
  assume_role_policy = "${data.aws_iam_policy_document.spot_fleet_assume_role.json}"
}
