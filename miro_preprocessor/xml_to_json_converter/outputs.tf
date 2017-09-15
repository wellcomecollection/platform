output "container_name" {
  value = "${module.xml_to_json_converter.container_name}"
}

output "task_definition_arn" {
  value = "${module.xml_to_json_converter.task_definition_arn}"
}
