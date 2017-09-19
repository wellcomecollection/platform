data "aws_iam_policy_document" "assume_spot_fleet_role" {
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

resource "aws_iam_role" "fleet_role" {
  name               = "fleet-role"
  assume_role_policy = "${data.aws_iam_policy_document.assume_spot_fleet_role.json}"
}

data "aws_iam_policy_document" "spot_fleet_permissions" {
  statement {
    actions = [
      "ec2:Describe*",
      "ec2:RequestSpotInstances",
      "iam:PassRole",
    ]

    resources = [
      "*",
    ]
  }
}

resource "aws_iam_role_policy" "give_spot_fleet_permissions" {
  role   = "${aws_iam_role.fleet_role.name}"
  policy = "${data.aws_iam_policy_document.spot_fleet_permissions.json}"

}
