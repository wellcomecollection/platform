variable "cluster_name" {
  description = "Name of cluster to run in"
}

variable "aws_region" {
  description = "AWS Region"
  default     = "eu-west-1"
}

variable "efs_filesystem_id" {
  description = "If the userdata requires an EFS mount point, this is it"
  default     = "no_name_set"
}

variable "ebs_block_device" {
  default = "no_name_set"
}

variable "cache_cleaner_cloudwatch_log_group" {
  default = ""
}