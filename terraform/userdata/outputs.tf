output "rendered" {
  value = "${data.template_file.template.rendered}"
}

output "efs_mount_directory" {
  value = "${local.mount_directory}"
}
