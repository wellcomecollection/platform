module "queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v11.6.0"
  queue_name  = "${replace(var.namespace,"-","_")}"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.account_id}"
  topic_names = ["${var.topic_names}"]

  visibility_timeout_seconds = "${var.visibility_timeout_seconds}"
  max_receive_count          = "${var.max_receive_count}"

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

resource "aws_sns_topic_subscription" "sns_topic" {
  count = "${length(var.topic_names)}"

  protocol  = "sqs"
  topic_arn = "${format("arn:aws:sns:%s:%s:%s", var.aws_region, var.account_id, element(var.topic_names, count.index))}"
  endpoint  = "${module.queue.arn}"
}

data "aws_iam_policy_document" "read_from_q" {
  statement {
    actions = [
      "sqs:DeleteMessage",
      "sqs:ReceiveMessage",
    ]

    resources = [
      "${module.queue.arn}",
    ]
  }
}

resource "aws_iam_role_policy" "read_from_q" {
  count = "${length(var.role_names)}"

  role   = "${var.role_names[count.index]}"
  policy = "${data.aws_iam_policy_document.read_from_q.json}"
}

resource "aws_iam_role_policy" "task_s3_messages_get" {
  count = "${length(var.role_names)}"

  role   = "${var.role_names[count.index]}"
  policy = "${data.aws_iam_policy_document.allow_s3_messages_get.json}"
}

data "aws_iam_policy_document" "allow_s3_messages_get" {
  statement {
    actions = [
      "s3:GetObject",
    ]

    resources = [
      "${var.messages_bucket_arn}/*",
    ]
  }
}
