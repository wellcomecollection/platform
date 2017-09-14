resource "aws_iam_role_policy" "xml_to_json_converter_read_from_s3" {
  role   = "${module.ecs_xml_to_json_converter_iam.task_role_name}"
  policy = "${var.s3_read_miro_data_json}"
}

resource "aws_iam_role_policy" "xml_to_json_converter_write_to_s3" {
  role   = "${module.ecs_xml_to_json_converter_iam.task_role_name}"
  policy = "${var.s3_write_miro_data_json}"
}
