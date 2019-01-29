data "aws_ssm_parameter" "api_release_uri" {
  name = "/releases/catalogue_api/latest/api"
}
