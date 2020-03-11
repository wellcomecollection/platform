variable "name" {
  description = "Name of the ASG to create"
}

variable "asg_min" {
  description = "Minimum number of instances"
  default     = "1"
}

variable "asg_desired" {
  description = "Desired number of instances"
  default     = "1"
}

variable "asg_max" {
  description = "Max number of instances"
  default     = "2"
}

variable "subnet_list" {
  type = "list"
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

variable "controlled_access_cidr_ingress" {
  type        = "list"
  default     = []
  description = "CIDR for SSH access to EC2 instances"
}

variable "ssh_ingress_security_groups" {
  type    = "list"
  default = []
}

variable "custom_security_groups" {
  type    = "list"
  default = []
}
