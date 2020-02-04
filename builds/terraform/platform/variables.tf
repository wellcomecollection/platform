variable "repo_name" {}
variable "infra_bucket_arn" {}
variable "sbt_releases_bucket_arn" {}
variable "platform_read_only_role" {}

variable "publish_topics" {
  type = list(string)
}
