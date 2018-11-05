resource "aws_iam_role_policy" "allow_gateway_s3_access" {
  policy = "${data.aws_iam_policy_document.archive_static_content_get.json}"
  role   = "${aws_iam_role.static_resource_role.id}"
}
