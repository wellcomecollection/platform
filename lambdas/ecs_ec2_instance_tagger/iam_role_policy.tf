resource "aws_iam_role_policy" "ecs_ec2_instance_tagger_write_tags" {
  role   = "${module.lambda_ecs_ec2_instance_tagger.role_name}"
  policy = "${data.aws_iam_policy_document.write_ec2_tags.json}"
}

resource "aws_iam_role_policy" "ecs_ec2_instance_tagger_use_tmp" {
  role   = "${module.lambda_ecs_ec2_instance_tagger.role_name}"
  policy = "${data.aws_iam_policy_document.s3_put_infra_tmp.json}"
}
