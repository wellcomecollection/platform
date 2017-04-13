data "template_file" "config" {
  template = "${file("${var.app_name}.conf.template")}"
  vars     = "${var.template_vars}"
}
