# This should probably be moved into a adapter account.
# Currently the platform account seems to be doing a decent job of this.

# This allows the reporting account to subscribe to the reindex topic
locals {
  platform_aws_id  = "760097843905"
  reporting_aws_id = "269807742353"
}

resource "aws_sns_topic_policy" "allow_reporting_subscription" {
  arn    = "${module.reporting_sierra_reindex_topic.arn}"
  policy = "${data.aws_iam_policy_document.sns-topic-policy.json}"
}

data "aws_iam_policy_document" "sns-topic-policy" {
  policy_id = "__default_policy_ID"

  statement {
    actions = [
      "SNS:Subscribe",
      "SNS:SetTopicAttributes",
      "SNS:RemovePermission",
      "SNS:Receive",
      "SNS:Publish",
      "SNS:ListSubscriptionsByTopic",
      "SNS:GetTopicAttributes",
      "SNS:DeleteTopic",
      "SNS:AddPermission",
    ]

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceOwner"

      values = [
        "760097843905",
      ]
    }

    effect = "Allow"

    principals {
      type        = "AWS"
      identifiers = ["*"]
    }

    resources = [
      "${module.reporting_sierra_reindex_topic.arn}",
    ]

    sid = "__default_statement_ID"
  }

  statement {
    effect = "Allow"

    actions = [
      "SNS:Subscribe",
      "SNS:ListSubscriptionsByTopic",
      "SNS:Receive",
      "SNS:GetTopicAttributes",
      "SNS:SetTopicAttributes",
    ]

    principals {
      type        = "AWS"
      identifiers = ["arn:aws:iam::${local.reporting_aws_id}:root"]
    }

    resources = [
      "${module.reporting_sierra_reindex_topic.arn}",
    ]

    sid = "ReportingAccess"
  }
}
