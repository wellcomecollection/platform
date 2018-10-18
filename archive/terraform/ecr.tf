data "aws_ecr_repository" "ecr_repository_nginx_services" {
  name = "uk.ac.wellcome/nginx_services"
}

module "ecr_repository_archivist" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "archivist"
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

module "ecr_repository_notifier" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "notifier"
}

module "ecr_repository_callback_stub_server" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "callback_stub_server"
}

module "ecr_repository_bagger" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "bagger"
}

module "ecr_repository_archive_api" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "archive_api"
}
