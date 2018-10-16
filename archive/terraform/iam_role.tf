resource "aws_iam_role" "dds_access" {
  name = "${local.namespace}-dds-access"
}
