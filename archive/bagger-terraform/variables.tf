variable "aws_region" {
  default = "eu-west-1"
}

variable "ec2_key_name" {
  default = "wellcomedigitalplatform"
}

variable "ec2_image_id" {
  default = "ami-0bdb1d6c15a40392c"
}

variable "vpc_cidr_block" {
  default = "10.0.0.0/16"
}
