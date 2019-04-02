# Miro Hybrid Store

output "vhs_miro_dynamodb_update_policy" {
  value = "${module.vhs_miro.dynamodb_update_policy}"
}

output "vhs_miro_read_policy" {
  value = "${module.vhs_miro.read_policy}"
}

output "vhs_miro_table_name" {
  value = "${module.vhs_miro.table_name}"
}

output "vhs_miro_bucket_name" {
  value = "${module.vhs_miro.bucket_name}"
}

# Miro Inventory Hybrid Store

output "vhs_miro_inventory_dynamodb_update_policy" {
  value = "${module.vhs_miro_migration.dynamodb_update_policy}"
}

output "vhs_miro_inventory_read_policy" {
  value = "${module.vhs_miro_migration.read_policy}"
}

output "vhs_miro_inventory_table_name" {
  value = "${module.vhs_miro_migration.table_name}"
}

output "vhs_miro_inventory_bucket_name" {
  value = "${module.vhs_miro_migration.bucket_name}"
}

# Sierra Hybrid Store

output "vhs_sierra_full_access_policy" {
  value = "${module.vhs_sierra.full_access_policy}"
}

output "vhs_sierra_dynamodb_update_policy" {
  value = "${module.vhs_sierra.dynamodb_update_policy}"
}

output "vhs_sierra_read_policy" {
  value = "${module.vhs_sierra.read_policy}"
}

output "vhs_sierra_table_name" {
  value = "${module.vhs_sierra.table_name}"
}

output "vhs_sierra_bucket_name" {
  value = "${module.vhs_sierra.bucket_name}"
}

# Sierra Items Hybrid Store

output "vhs_sierra_items_full_access_policy" {
  value = "${module.vhs_sierra_items.full_access_policy}"
}

output "vhs_sierra_items_table_name" {
  value = "${module.vhs_sierra_items.table_name}"
}

output "vhs_sierra_items_bucket_name" {
  value = "${module.vhs_sierra_items.bucket_name}"
}

# Goobi Hybrid Store

output "vhs_goobi_full_access_policy" {
  value = "${module.vhs_goobi_mets.full_access_policy}"
}

output "vhs_goobi_table_name" {
  value = "${module.vhs_goobi_mets.table_name}"
}

output "vhs_goobi_bucket_name" {
  value = "${module.vhs_goobi_mets.bucket_name}"
}

# RDS

output "rds_access_security_group_id" {
  value = "${aws_security_group.rds_ingress_security_group.id}"
}

output "identifiers_rds_cluster_password" {
  value = "${module.identifiers_rds_cluster.password}"
}

output "identifiers_rds_cluster_username" {
  value = "${module.identifiers_rds_cluster.username}"
}

output "identifiers_rds_cluster_port" {
  value = "${module.identifiers_rds_cluster.port}"
}

output "identifiers_rds_cluster_host" {
  value = "${module.identifiers_rds_cluster.host}"
}

# Cognito

output "cognito_user_pool_arn" {
  value = "${aws_cognito_user_pool.pool.arn}"
}

output "cognito_storage_api_identifier" {
  value = "${aws_cognito_resource_server.storage_api.identifier}"
}

# Misc

output "admin_cidr_ingress" {
  value = "${local.admin_cidr_ingress}"
}
