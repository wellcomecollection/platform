variable "release_ids" {
  description = "Release tags for platform apps"
  type        = "map"
}

variable "key_name" {
  description = "Name of AWS key pair"
}

variable "admin_cidr_ingress" {
  description = "CIDR to allow tcp/22 ingress to EC2 instance"
}

variable "infra_bucket" {
  description = "S3 bucket storing our configuration"
}

variable "aws_region" {
  description = "The AWS region to create things in."
  default     = "eu-west-1"
}

variable "rds_username" {
  description = "Username for the RDS instance"
}

variable "rds_password" {
  description = "Password7 for the RDS database"
}

variable "es_cluster_credentials" {
  description = "Credentials for the Elasticsearch cluster"
  type        = "map"
}

variable "matcher_graph_table_index" {
  description = "Name of the GSI in the matcher graph table"
  default     = "work-sets-index"
}

variable "matcher_lock_table_index" {
  description = "Name of the GSI in the matcher lock table"
  default     = "context-ids-index"
}
