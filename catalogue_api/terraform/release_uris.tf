data "aws_ssm_parameter" "api_release_uri" {
  name = "/releases/catalogue_api/latest/api"
}

data "aws_ssm_parameter" "api_nginx_release_uri" {
  name = "/releases/catalogue_api/latest/nginx_api-gw"
}

data "aws_ssm_parameter" "snapshot_generator_release_uri" {
  name = "/releases/data_api/latest/snapshot_generator"
}

data "aws_ssm_parameter" "update_api_docs_release_uri" {
  name = "/releases/catalogue_api/latest/update_api_docs"
}
