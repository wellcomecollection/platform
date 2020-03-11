variable "name" {
  description = "Name of the ASG to create"
}

variable "subnet_list" {
  type = list(string)
}

variable "instance_type" {
  default     = "t2.small"
  description = "AWS instance type"
}

variable "key_name" {
  description = "SSH key pair name for instance sign-in"
}

variable "user_data" {
  description = "User data for ec2 container hosts"
  default     = ""
}

variable "associate_public_ip_address" {
  description = "Associate public IP address?"
  default     = true
}

variable "vpc_id" {
  description = "VPC for EC2 autoscaling group security group"
}

variable "image_id" {
  description = "ID of the AMI to use on the instances"
}

variable "custom_security_groups" {
  type    = list(string)
  default = []
}
