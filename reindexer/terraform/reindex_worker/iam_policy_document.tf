data "aws_iam_policy_document" "allow_cloudwatch_push_metrics" {
  statement {
    actions = [
      "cloudwatch:PutMetricData",
    ]

    resources = [
      "*",
    ]
  }
}

data "aws_iam_policy_document" "vhs_read_policy" {
  statement {
    actions = [
      "dynamodb:Scan",
    ]

    resources = [
      "${local.reindexer_tables}",
    ]
  }
}

data "aws_iam_policy_document" "sns_publish_policy" {
  statement {
    actions = [
      "sns:Publish",
    ]

    resources = [
      "${local.reindexer_topics}",
    ]
  }
}

# This block of interpolation syntax gets a list of all the table ARNs that the
# reindexer is configured to be able to read from.
#

data "template_file" "table_name" {
  count    = "${length(var.reindexer_jobs)}"
  template = "arn:aws:dynamodb:${var.aws_region}:${var.account_id}:table/$${table}"

  vars = {
    table = "${lookup(var.reindexer_jobs[count.index], "table")}"
  }
}

locals {
  reindexer_tables = "${distinct(data.template_file.table_name.*.rendered)}"
}

# This block of interpolation syntax gets a list of all the topic ARNs that the
# reindexer is configured to be able to publish to.
#

data "template_file" "topic_arn" {
  count    = "${length(var.reindexer_jobs)}"
  template = "${lookup(var.reindexer_jobs[count.index], "topic")}"
}

locals {
  reindexer_topics = "${distinct(data.template_file.topic_arn.*.rendered)}"
}
