variable "key_name" {
  description = "Name of AWS key pair"
}

variable "admin_cidr_ingress" {
  description = "CIDR to allow tcp/22 ingress to EC2 instance"
}

variable "release_ids" {
  description = "Release tags for platform apps"
  type        = "map"
}