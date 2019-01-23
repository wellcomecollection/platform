# ECR

module "ecr_repository_nginx_api-gw" {
  source    = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v19.5.1"
  id        = "nginx_api-gw"
  namespace = "uk.ac.wellcome"
}

resource "aws_ecr_repository_policy" "nginx_api_gw" {
  repository = "${module.ecr_repository_nginx_api-gw.name}"
  policy     = "${data.aws_iam_policy_document.storage_get_images.json}"
}

data "aws_iam_policy_document" "storage_get_images" {
  statement {
    actions = [
      "ecr:GetDownloadUrlForLayer",
      "ecr:BatchGetImage",
      "ecr:BatchCheckLayerAvailability",
    ]

    principals {
      identifiers = ["arn:aws:iam::975596993436:root"]
      type        = "AWS"
    }
  }
}

module "ecr_repository_api" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "api"
}

module "ecr_repository_update_api_docs" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "update_api_docs"
}

# ECS Cluster

resource "aws_ecs_cluster" "cluster" {
  name = "${local.namespace}"
}
