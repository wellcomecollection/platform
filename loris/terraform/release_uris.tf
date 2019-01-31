data "aws_ssm_parameter" "loris_release_uri" {
  name = "/releases/loris/latest/loris"
}

data "aws_ssm_parameter" "nginx_loris_release_uri" {
  name = "/releases/loris/latest/nginx_loris-delta"
}
