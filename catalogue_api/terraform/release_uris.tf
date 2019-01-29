data "aws_ssm_parameter" "api_release_uri" {
  name = "/releases/catalogue_api/latest/api"
}

data "aws_ssm_parameter" "snapshot_generator_release_uri" {
  name = "/releases/data_api/latest/snapshot_generator"
}
