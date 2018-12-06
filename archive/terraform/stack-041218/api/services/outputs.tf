output "ingests_name" {
  value = "${module.ingests.name}"
}

output "ingests_role_name" {
  value = "${module.ingests.task_role_name}"
}

output "bags_name" {
  value = "${module.bags.name}"
}

output "bags_role_name" {
  value = "${module.bags.task_role_name}"
}
