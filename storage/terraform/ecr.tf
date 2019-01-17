module "ecr_repository_archivist" {
  source    = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v19.5.1"
  id        = "archivist"
  namespace = "uk.ac.wellcome"
}

resource "aws_ecr_repository_policy" "archivist" {
  repository = "${module.ecr_repository_archivist.name}"
  policy     = "${data.aws_iam_policy_document.storage_get_images.json}"
}

module "ecr_repository_bags" {
  source    = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v19.5.1"
  id        = "bags"
  namespace = "uk.ac.wellcome"
}

resource "aws_ecr_repository_policy" "bags" {
  repository = "${module.ecr_repository_bags.name}"
  policy     = "${data.aws_iam_policy_document.storage_get_images.json}"
}

module "ecr_repository_bags_api" {
  source    = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v19.5.1"
  id        = "bags_api"
  namespace = "uk.ac.wellcome"
}

resource "aws_ecr_repository_policy" "bags_api" {
  repository = "${module.ecr_repository_bags_api.name}"
  policy     = "${data.aws_iam_policy_document.storage_get_images.json}"
}

module "ecr_repository_ingests" {
  source    = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v19.5.1"
  id        = "ingests"
  namespace = "uk.ac.wellcome"
}

resource "aws_ecr_repository_policy" "ingests" {
  repository = "${module.ecr_repository_ingests.name}"
  policy     = "${data.aws_iam_policy_document.storage_get_images.json}"
}

module "ecr_repository_ingests_api" {
  source    = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v19.5.1"
  id        = "ingests_api"
  namespace = "uk.ac.wellcome"
}

resource "aws_ecr_repository_policy" "ingests_api" {
  repository = "${module.ecr_repository_ingests_api.name}"
  policy     = "${data.aws_iam_policy_document.storage_get_images.json}"
}

module "ecr_repository_notifier" {
  source    = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v19.5.1"
  id        = "notifier"
  namespace = "uk.ac.wellcome"
}

resource "aws_ecr_repository_policy" "notifier" {
  repository = "${module.ecr_repository_notifier.name}"
  policy     = "${data.aws_iam_policy_document.storage_get_images.json}"
}

module "ecr_repository_bag_replicator" {
  source    = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v19.5.1"
  id        = "bag_replicator"
  namespace = "uk.ac.wellcome"
}

resource "aws_ecr_repository_policy" "bag_replicator" {
  repository = "${module.ecr_repository_bag_replicator.name}"
  policy     = "${data.aws_iam_policy_document.storage_get_images.json}"
}

module "ecr_repository_bagger" {
  source    = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v19.5.1"
  id        = "bagger"
  namespace = "uk.ac.wellcome"
}

resource "aws_ecr_repository_policy" "bagger" {
  repository = "${module.ecr_repository_bagger.name}"
  policy     = "${data.aws_iam_policy_document.storage_get_images.json}"
}

module "ecr_repository_callback_stub_server" {
  source    = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v19.5.1"
  id        = "callback_stub_server"
  namespace = "uk.ac.wellcome"
}

resource "aws_ecr_repository_policy" "callback_stub_server" {
  repository = "${module.ecr_repository_callback_stub_server.name}"
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

# TODO: Delete this repos when publishing to new ones

module "ecr_repository_archive_api" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "archive_api"
}

module "ecr_repository_registrar_async" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "registrar_async"
}

module "ecr_repository_registrar_http" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "registrar_http"
}

module "ecr_repository_progress_async" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "progress_async"
}

module "ecr_repository_progress_http" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "progress_http"
}
