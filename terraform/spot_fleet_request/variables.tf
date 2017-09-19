variable "spot_price" {}
variable "instance_type" {}
variable "image_id" {}
variable "key_name" {}
variable "user_data" {}

variable "availability_zone" {
  default = "eu-west-1"
}

variable "target_capacity" {
  default = 1
}

variable "valid_until" {
  default = "2019-11-04T20:44:20Z"
}
