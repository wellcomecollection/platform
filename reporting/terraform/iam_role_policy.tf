resource "aws_iam_role_policy" "miro_vhs_read_transformer_example" {
  role   = "${module.lambda_miro_transformer.role_name}"
  policy = "${local.miro_vhs_read_policy}"
<<<<<<< HEAD
}
=======
}
>>>>>>> d6f90e184d60637d17aea899bcb2af5071603728
