/* TODO: Scope this policy document more tightly */
data "aws_iam_policy_document" "allow_dynamodb_all" {
  statement {
    actions = [
      "dynamodb:*",
    ]

    resources = [
      "*",
    ]
  }
}

data "aws_iam_policy_document" "allow_calm_db_all" {
  statement {
    actions = [
      "dynamodb:DescribeTable",
      "dynamodb:PutItem",
      "dynamodb:UpdateTable",
    ]

    resources = [
      "${aws_dynamodb_table.calm_table.arn}",
    ]
  }
}

data "aws_iam_policy_document" "allow_table_capacity_changes" {
  statement {
    actions = [
      "dynamodb:DescribeTable",
      "dynamodb:UpdateTable",
    ]

    resources = [
      "*",
    ]
  }
}

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

data "aws_iam_policy_document" "read_calm_kinesis_stream" {
  statement {
    actions = [
      "dynamodb:*",
    ]

    resources = [
      "${aws_dynamodb_table.calm_table.stream_arn}",
    ]
  }
}

data "aws_iam_policy_document" "update_ecs_service_size" {
  statement {
    actions = [
      "ecs:UpdateService",
    ]

    resources = [
      "*",
    ]
  }
}

data "aws_iam_policy_document" "stop_running_tasks" {
  statement {
    actions = [
      "ecs:DescribeServices",
      "ecs:DescribeTaskDefinition",
      "ecs:ListClusters",
      "ecs:ListServices",
      "ecs:RegisterTaskDefinition",
      "ecs:UpdateService",
      "iam:PassRole",
    ]

    resources = [
      "*",
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

data "aws_iam_policy_document" "travis_permissions" {
  statement {
    actions = [
      "ecr:*",
    ]

    resources = [
      "*",
    ]
  }

  statement {
    actions = [
      "ecr:GetAuthorizationToken",
    ]

    resources = [
      "*",
    ]
  }

  statement {
    actions = [
      "s3:PutObject",
      "s3:GetObject",
    ]

    resources = [
      "${aws_s3_bucket.infra.arn}/releases/*",
    ]
  }
}

data "aws_iam_policy_document" "miro_images_sync" {
  statement {
    # Easier than listing the _many_ permissions required for s3 sync to work
    actions = [
      "s3:*",
    ]

    resources = [
      "${aws_s3_bucket.miro-images-sync.arn}/*",
      "${aws_s3_bucket.miro-images-sync.arn}",
      "${aws_s3_bucket.miro_images_public.arn}/*",
      "${aws_s3_bucket.miro_images_public.arn}",
    ]
  }

  statement {
    actions = [
      "s3:ListAllMyBuckets",
    ]

    resources = [
      "arn:aws:s3:::*",
    ]
  }
}

data "aws_iam_policy_document" "s3_put_dashboard_status" {
  statement {
    actions = [
      "s3:PutObject",
      "s3:GetObjectACL",
      "s3:PutObjectACL",
    ]

    resources = [
      "${aws_s3_bucket.dashboard.arn}/data/*",
    ]
  }
}

data "aws_iam_policy_document" "write_ec2_tags" {
  statement {
    actions = [
      "tag:addResourceTags",
      "tag:removeResourceTags",
      "tag:tagResources",
      "tag:untagResources",
    ]

    resources = [
      "arn:aws:ec2:*",
    ]
  }
}
