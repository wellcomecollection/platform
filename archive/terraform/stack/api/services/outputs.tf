output "ingests_role_name" {
  value = "${module.ingests.task_role_name}"
}

output "bags_role_name" {
  value = "${module.bags.task_role_name}"
}
