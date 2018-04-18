# Outputs required for Loris

output "ecs_ami_id" {
  value = "${data.aws_ami.stable_coreos.id}"
}

# Outputs required for the Elasticsearch snapshot service

output "prod_es_name" {
  value = "${var.production_api == "remus" ? var.es_config_remus["name"] : var.es_config_romulus["name"]}"
}

output "prod_es_region" {
  value = "${var.production_api == "remus" ? var.es_config_remus["region"] : var.es_config_romulus["region"]}"
}

output "prod_es_port" {
  value = "${var.production_api == "remus" ? var.es_config_remus["port"] : var.es_config_romulus["port"]}"
}

output "prod_es_index_v1" {
  value = "${var.production_api == "remus" ? var.es_config_remus["index_v1"] : var.es_config_romulus["index_v1"]}"
}

output "prod_es_index_v2" {
  value = "${var.production_api == "remus" ? var.es_config_remus["index_v2"] : var.es_config_romulus["index_v2"]}"
}

output "prod_es_doc_type" {
  value = "${var.production_api == "remus" ? var.es_config_remus["doc_type"] : var.es_config_romulus["doc_type"]}"
}

output "prod_es_username" {
  value = "${var.production_api == "remus" ? var.es_config_remus["username"] : var.es_config_romulus["username"]}"
}

output "prod_es_password" {
  value = "${var.production_api == "remus" ? var.es_config_remus["password"] : var.es_config_romulus["password"]}"
}
