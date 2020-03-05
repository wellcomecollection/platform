locals {
  namespace = "uk.ac.wellcome"
}

// TODO: Ensure this is accessible from experience!
resource "aws_ecr_repository" "nginx_experience" {
  name = "${local.namespace}/nginx_experience"
}

resource "aws_ecr_repository" "nginx_loris" {
  name = "${local.namespace}/nginx_loris"
}

resource "aws_ecr_repository" "nginx_grafana" {
  name = "${local.namespace}/nginx_grafana"
}

resource "aws_ecr_repository" "nginx_apigw" {
  name = "${local.namespace}/nginx_apigw"
}

resource "aws_ecr_repository_policy" "callback_stub_server" {
  repository = aws_ecr_repository.nginx_apigw.id
  policy = data.aws_iam_policy_document.storage_get_images.json
}

data "aws_iam_policy_document" "storage_get_images" {
  statement {
    actions = [
      "ecr:GetDownloadUrlForLayer",
      "ecr:BatchGetImage",
      "ecr:BatchCheckLayerAvailability",
    ]

    principals {
      identifiers = [
        "arn:aws:iam::975596993436:root"]
      type = "AWS"
    }
  }
}

data "aws_iam_policy_document" "storage_get_images" {
  statement {
    actions = [
      "ecr:GetDownloadUrlForLayer",
      "ecr:BatchGetImage",
      "ecr:BatchCheckLayerAvailability",
    ]

    principals {
      identifiers = [
        "arn:aws:iam::975596993436:root"]
      type = "AWS"
    }
  }
}