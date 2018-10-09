variable "release_ids" {
  description = "Release tags for Miro preprocessor"
  type        = "map"
}

variable "aws_region" {
  default = "eu-west-1"
}

variable "bucket_miro_data_id" {}
variable "s3_read_miro_data_json" {}
variable "s3_write_miro_data_json" {}
