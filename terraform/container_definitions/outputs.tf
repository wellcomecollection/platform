output "rendered" {
  value = "${data.template_file.definition.rendered}"
}

output "volume_name" {
  value = "${data.template_file.definition.vars.volume_name}"
}

output "name" {
  value = "${data.template_file.definition.vars.name}"
}