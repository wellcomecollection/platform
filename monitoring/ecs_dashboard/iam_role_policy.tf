resource "aws_iam_role_policy" "update_service_list_describe_services" {
  role   = "${module.update_service_list.role_name}"
  policy = "${data.aws_iam_policy_document.describe_services.json}"
}

resource "aws_iam_role_policy" "update_service_list_push_to_s3" {
  role   = "${module.update_service_list.role_name}"
  policy = "${data.aws_iam_policy_document.s3_put_dashboard_status.json}"
}

resource "aws_iam_role_policy" "update_service_list_read_from_webplatform" {
  role   = "${module.update_service_list.role_name}"
  policy = "${data.aws_iam_policy_document.assume_roles.json}"
}
