variable "aws_region" {
  description = "The AWS region to create things in."
  default     = "eu-west-1"
}

variable "rds_username" {
  description = "Username for the RDS instance"
}

variable "rds_password" {
  description = "Password for the RDS database"
}

variable "admin_cidr_ingress" {
  description = "CIDR to allow tcp/22 ingress to EC2 instance"
}
