data "aws_ssm_parameter" "infra_bucket" {
  name = "/infra_shared/config/prod/infra_bucket"
}
