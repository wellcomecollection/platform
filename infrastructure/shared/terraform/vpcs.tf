# Used by:
# - Catalogue Pipeline
# - Catalogue API
# - IIIF Image server (Loris)
# - Reindexer
# - Sierra Adapter

module "catalogue_vpc_delta" {
  source = "github.com/wellcometrust/terraform//network/prebuilt/vpc/public-private-igw?ref=d92bce2"

  name = "catalogue-172-31-0-0-16"

  cidr_block_vpc = "172.31.0.0/16"

  public_az_count           = "3"
  cidr_block_public         = "172.31.0.0/17"
  cidrsubnet_newbits_public = "2"

  private_az_count           = "3"
  cidr_block_private         = "172.31.128.0/17"
  cidrsubnet_newbits_private = "2"
}

# Used by:
# - Storage service

locals {
  storage_cidr_block_vpc     = "172.30.0.0/16"
  storage_cidr_block_public  = "172.30.0.0/17"
  storage_cidr_block_private = "172.30.128.0/17"
}

module "storage_vpc" {
  source = "github.com/wellcometrust/terraform//network/prebuilt/vpc/public-private-igw?ref=v19.3.0"

  name = "storage-172-30-0-0-16"

  cidr_block_vpc = "${local.storage_cidr_block_vpc}"

  public_az_count           = "3"
  cidr_block_public         = "${local.storage_cidr_block_public}"
  cidrsubnet_newbits_public = "2"

  private_az_count           = "3"
  cidr_block_private         = "${local.storage_cidr_block_private}"
  cidrsubnet_newbits_private = "2"

  providers = {
    aws = "aws.storage"
  }
}

# Used by:
# - Grafana service
# - Various monitoring lambdas

module "monitoring_vpc_delta" {
  source = "github.com/wellcometrust/terraform//network/prebuilt/vpc/public-private-igw?ref=d92bce2"

  name = "monitoring-172-28-0-0-16"

  cidr_block_vpc = "172.28.0.0/16"

  public_az_count           = "3"
  cidr_block_public         = "172.28.0.0/17"
  cidrsubnet_newbits_public = "2"

  private_az_count           = "3"
  cidr_block_private         = "172.28.128.0/17"
  cidrsubnet_newbits_private = "2"
}

# Used by:
# - Data science service
# - Labs apps & data scientist infra

module "datascience_vpc_delta" {
  source = "github.com/wellcometrust/terraform//network/prebuilt/vpc/public-private-igw?ref=d92bce2"

  name = "datascience-172-27-0-0-16"

  cidr_block_vpc = "172.27.0.0/16"

  public_az_count           = "3"
  cidr_block_public         = "172.27.0.0/17"
  cidrsubnet_newbits_public = "2"

  private_az_count           = "3"
  cidr_block_private         = "172.27.128.0/17"
  cidrsubnet_newbits_private = "2"
}
