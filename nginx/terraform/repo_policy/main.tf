resource "aws_ecr_repository_policy" "cross_account_policy" {
  repository = var.repo_name
  policy = data.aws_iam_policy_document.get_images.json
}

data "aws_iam_policy_document" "get_images" {
  statement {
    actions = [
      "ecr:GetDownloadUrlForLayer",
      "ecr:BatchGetImage",
      "ecr:BatchCheckLayerAvailability",
    ]

    principals {
      identifiers = ["arn:aws:iam::${var.account_id}:root"]
      type = "AWS"
    }
  }
}