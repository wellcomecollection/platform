data "aws_ssm_parameter" "loris_release_uri" {
  name = "/releases/loris/latest/loris"
}
