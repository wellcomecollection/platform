locals {
  id_minter_image = "${data.aws_ssm_parameter.id_minter_release_uri.value}"
  recorder_image  = "${data.aws_ssm_parameter.recorder_release_uri.value}"
  matcher_image   = "${data.aws_ssm_parameter.matcher_release_uri.value}"
  merger_image    = "${data.aws_ssm_parameter.merger_release_uri.value}"
  ingestor_image  = "${data.aws_ssm_parameter.ingestor_release_uri.value}"

  transformer_miro_image   = "${data.aws_ssm_parameter.transformer_miro_release_uri.value}"
  transformer_sierra_image = "${data.aws_ssm_parameter.transformer_sierra_release_uri.value}"
}

data "aws_ssm_parameter" "id_minter_release_uri" {
  name = "/releases/catalogue_pipeline/${var.release_label}/id_minter"
}

data "aws_ssm_parameter" "recorder_release_uri" {
  name = "/releases/catalogue_pipeline/${var.release_label}/recorder"
}

data "aws_ssm_parameter" "matcher_release_uri" {
  name = "/releases/catalogue_pipeline/${var.release_label}/matcher"
}

data "aws_ssm_parameter" "merger_release_uri" {
  name = "/releases/catalogue_pipeline/${var.release_label}/merger"
}

data "aws_ssm_parameter" "ingestor_release_uri" {
  name = "/releases/catalogue_pipeline/${var.release_label}/ingestor"
}

data "aws_ssm_parameter" "transformer_miro_release_uri" {
  name = "/releases/catalogue_pipeline/transformer/${var.release_label}/transformer_miro"
}

data "aws_ssm_parameter" "transformer_sierra_release_uri" {
  name = "/releases/catalogue_pipeline/transformer/${var.release_label}/transformer_sierra"
}
