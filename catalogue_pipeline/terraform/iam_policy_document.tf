data "aws_iam_policy_document" "reindex_tracker_table" {
  statement {
    actions = [
      "dynamodb:*",
    ]

    resources = [
      "${aws_dynamodb_table.reindex_tracker.arn}",
    ]
  }
}

data "aws_iam_policy_document" "reindex_target_miro" {
  statement {
    actions = [
      "dynamodb:*",
    ]

    resources = [
      "${aws_dynamodb_table.miro_table.arn}",
      "${aws_dynamodb_table.miro_table.arn}/index/ReindexTracker",
    ]
  }
}

data "aws_iam_policy_document" "s3_read_ingestor_config" {
  statement {
    actions = [
      "s3:GetObject",
    ]

    resources = [
      "arn:aws:s3:::${var.infra_bucket}/${module.ingestor.config_key}",
    ]
  }
}

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

data "aws_iam_policy_document" "allow_cloudwatch_read_metrics" {
  statement {
    actions = [
      "cloudwatch:DescribeAlarmHistory",
      "cloudwatch:DescribeAlarms",
      "cloudwatch:DescribeAlarmsForMetric",
      "cloudwatch:GetMetricData",
      "cloudwatch:GetMetricStatistics",
      "cloudwatch:ListMetrics",
    ]

    resources = [
      "*",
    ]
  }
}

data "aws_iam_policy_document" "s3_upload_to_to_elasticdump_directory" {
  statement {
    actions = [
      "s3:PutObject",
    ]

    resources = [
      "arn:aws:s3:::${var.infra_bucket}/elasticdump/*",
    ]
  }
}
