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
  default     = ""
}

variable "associate_public_ip_address" {
  description = "Associate public IP address?"
}

variable "instance_security_groups" {
  type    = "list"
  default = []
}
