module "ecr_repository_nginx_loris" {
  source    = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v19.5.1"
  id        = "nginx_loris"
  namespace = "uk.ac.wellcome"
}

module "ecr_repository_nginx_grafana" {
  source    = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v19.5.1"
  id        = "nginx_grafana"
  namespace = "uk.ac.wellcome"
}

module "ecr_repository_nginx_apigw" {
  source    = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v19.5.1"
  id        = "nginx_apigw"
  namespace = "uk.ac.wellcome"
}

resource "aws_ecr_repository_policy" "callback_stub_server" {
  repository = "${module.ecr_repository_nginx_apigw.name}"
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
