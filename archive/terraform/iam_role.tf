resource "aws_iam_role" "dds_access" {
  name               = "${local.namespace}-dds-access"
  assume_role_policy = "${data.aws_iam_policy_document.ec2_instance_assume_role.json}"
}
