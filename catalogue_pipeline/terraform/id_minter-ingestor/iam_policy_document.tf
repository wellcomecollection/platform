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