resource "aws_iam_role_policy" "publish_to_sns" {
  name = "publish_to_sns"
  role = "${aws_iam_role.role.id}"

  policy = "${var.publish_to_sns_policy}"
}


resource "aws_iam_role" "role" {
  name               = "${var.asg_name}_role"
  assume_role_policy = "${data.aws_iam_policy_document.assume_ec2_role.json}"
}

data "aws_iam_policy_document" "assume_ec2_role" {
  statement {
    actions = [
      "sts:AssumeRole",
    ]

    principals {
      type        = "Service"
      identifiers = ["autoscaling.amazonaws.com"]
    }
  }
}