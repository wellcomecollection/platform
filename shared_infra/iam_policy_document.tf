data "aws_iam_policy_document" "alb_logs" {
  statement {
    actions = [
      "s3:PutObject",
    ]

    resources = [
      "arn:aws:s3:::wellcomecollection-alb-logs/*",
    ]

    principals {
      identifiers = ["arn:aws:iam::156460612806:root"]
      type = "AWS"
    }
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

data "aws_iam_policy_document" "s3_mets_ingest_bucket_read_write" {
  statement {
    actions = [
      "s3:*",
    ]

    resources = [
      "${aws_s3_bucket.mets-ingest.arn}/*",
      "${aws_s3_bucket.mets-ingest.arn}",
    ]
  }
}

data "aws_iam_policy_document" "s3_wellcomecollection_mets_ingest_bucket_read_write" {
  statement {
    actions = [
      "s3:*",
    ]

    resources = [
      "${aws_s3_bucket.wellcomecollection-mets-ingest.arn}/*",
      "${aws_s3_bucket.wellcomecollection-mets-ingest.arn}",
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

data "aws_iam_policy_document" "s3_put_gatling_reports" {
  statement {
    actions = [
      "s3:PutObject",
      "s3:GetObjectACL",
      "s3:PutObjectACL",
    ]

    resources = [
      "${aws_s3_bucket.dashboard.arn}/gatling/*",
    ]
  }
}

data "aws_iam_policy_document" "s3_read_miro_images" {
  statement {
    actions = [
      "s3:Get*",
      "s3:List*",
    ]

    resources = [
      "${aws_s3_bucket.miro_images_public.arn}",
    ]
  }
}

data "aws_iam_policy_document" "s3_tif_derivative" {
  statement {
    actions = [
      "s3:*",
    ]

    resources = [
      "arn:aws:s3:::wellcomecollection-tif-derivatives/*",
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
