data "aws_iam_policy_document" "assume_autoscaling_role" {
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
