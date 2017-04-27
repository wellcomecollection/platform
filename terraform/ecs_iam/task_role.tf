resource "aws_iam_role" "task_role" {
  name               = "${var.name}_task_role"
  assume_role_policy = "${data.aws_iam_policy_document.assume_ecs_role.json}"
}

data "aws_iam_policy_document" "assume_ecs_role" {
  statement {
    actions = [
      "sts:AssumeRole",
    ]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role_policy" "s3_role" {
  name   = "${var.name}_read_from_s3"
  role   = "${aws_iam_role.task_role.name}"
  policy = "${data.aws_iam_policy_document.allow_s3_read.json}"
}

# Our applications read their config from S3 on startup, make sure they
# have the appropriate read permissions.
# TODO: Scope these more tightly, possibly at the bucket or even object level.
data "aws_iam_policy_document" "allow_s3_read" {
  statement {
    actions = [
      "s3:Get*",
      "s3:List*",
    ]

    resources = [
      "*",
    ]
  }
}
