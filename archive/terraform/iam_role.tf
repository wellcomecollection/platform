resource "aws_iam_role" "dds_access" {
  name               = "${local.namespace}-dds-access"
  assume_role_policy = "${data.aws_iam_policy_document.dds_assume_role.json}"
}
