variable "name" {
  description = "Name of the EFS mount"
}

variable "subnets" {
  type        = "list"
  description = "subnets where to create the EFS mount"
}

variable "vpc_id" {
  description = "ID of VPC to to create EFS mount in"
}

variable "efs_access_security_group_id" {
  description = "ID of the security group of the EC2 instaces that need to access the EFS"
}
