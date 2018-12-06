module "topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v1.0.0"
  name   = "${replace(var.namespace,"-","")}"
}

resource "aws_iam_role_policy" "policy" {
  count = "${length(var.role_names)}"

  role   = "${var.role_names[count.index]}"
  policy = "${module.topic.publish_policy}"
}
