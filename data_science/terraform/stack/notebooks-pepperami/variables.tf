variable "key_name" {}
variable "s3_bucket_name" {}
variable "s3_bucket_arn" {}
variable "ebs_volume_size" {
  default = 250
}
variable "vpc_id" {}
variable "controlled_access_cidr_ingress" {
  type        = "list"
  default     = []
  description = "CIDR for SSH access to EC2 instances"
}
variable "efs_security_group_id" {}
variable "efs_id" {}
variable "namespace" {}
variable "instance_type" {
  default     = "t3.xlarge"
  description = "AWS instance type"
}