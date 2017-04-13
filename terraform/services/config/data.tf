data "template_file" "config" {
  template = "${file("${path.module}/templates/${var.app_name}.conf.template")}"
  vars     = "${var.template_vars}"
}
