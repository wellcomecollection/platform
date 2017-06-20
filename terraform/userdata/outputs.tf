output "rendered" {
  value = "${data.template_file.template.rendered}"
}

output "efs_mount_directory" {
  value = "${data.template_file.template.vars.efs_mount_directory}"
}
