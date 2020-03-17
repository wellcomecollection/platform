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

// Cross account access policies

module "storage_repo_policy" {
  source = "./repo_policy"

  account_id = local.storage_account_id
  repo_name  = aws_ecr_repository.nginx_apigw.name
}

module "experience_repo_policy" {
  source = "./repo_policy"

  account_id = local.experience_account_id
  repo_name  = aws_ecr_repository.nginx_experience.name
}

module "catalogue_repo_policy" {
  source = "./repo_policy"

  account_id = local.catalogue_account_id
  repo_name  = aws_ecr_repository.nginx_apigw.name
}
