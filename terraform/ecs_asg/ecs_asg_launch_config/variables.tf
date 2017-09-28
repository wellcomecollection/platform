variable "vpc_id" {
  description = "VPC for EC2 autoscaling group security group"
}

variable "asg_name" {
  description = "Name of the ASG to create"
}

variable "key_name" {
  description = "SSH key pair name for instance sign-in"
}

variable "image_id" {
  description = "ID of the AMI to use on the instances"
}

variable "instance_type" {
  description = "AWS instance type"
}

variable "instance_profile_name" {
  description = "Instance profile for ec2 container hosts"
}

variable "user_data" {
  description = "User data for ec2 container hosts"
}

variable "public_ip" {
  description = "Associate public IP address?"
}

variable "admin_cidr_ingress" {
  description = "CIDR for SSH access to EC2 instances"
}

variable "random_key" {
  default = "initial"
}

variable "use_spot" {}
variable spot_price {}


variable "ebs_device_name" {
}

variable "ebs_size" {
}
variable "ebs_volume_type" {
}