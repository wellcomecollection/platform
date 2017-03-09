variable "template_name" {
  description = "CoreOS cloud config template name to use"
  default     = "ecs-agent"
}

variable "cluster_name" {
  description = "Name of cluster to run in"
}

variable "aws_region" {
  description = "AWS Region"
  default     = "eu-west-1"
}

variable "efs_fs_name" {
  description = "If the userdata requires an EFS mount point, this is it"
  default     = "no_name_set"
}
