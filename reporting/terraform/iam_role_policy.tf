resource "aws_iam_role_policy" "miro_vhs_read_transformer_example" {
  role   = "${module.lambda_miro_transformer.role_name}"
  policy = "${local.miro_vhs_read_policy}"
}