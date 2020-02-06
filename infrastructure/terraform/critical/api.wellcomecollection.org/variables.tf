variable "subdomain" {
  description = "A subdomain implementing the wellcomecollection.org APIs (probably api./api-stage.)"
}

variable "cert_domain" {
  description = "The primary certificate domain for these APIs (probably api.wellcomecollection.org"
}

variable "public_api_bucket_domain_name" {}

variable "description" {}

variable "cf_logging_bucket" {}
