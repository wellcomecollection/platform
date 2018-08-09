variable "name" {}

variable "bucket_name_prefix" {
  default = "wellcomecollection-vhs-"
}

variable "table_name_prefix" {
  default = "vhs-"
}

variable "table_read_max_capacity" {
  default = 80
}

variable "table_write_max_capacity" {
  default = 300
}
