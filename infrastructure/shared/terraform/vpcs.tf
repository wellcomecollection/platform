module "catalogue_vpc_delta" {
  source = "github.com/wellcometrust/terraform//network/prebuilt/vpc/public-private-igw?ref=v16.1.0"

  name = "catalogue-172-31-0-0-16"

  cidr_block_vpc = "172.31.0.0/16"

  cidr_block_public         = "172.31.0.0/17"
  cidrsubnet_newbits_public = "2"

  cidr_block_private         = "172.31.128.0/17"
  cidrsubnet_newbits_private = "2"
}

module "storage_vpc_delta" {
  source = "github.com/wellcometrust/terraform//network/prebuilt/vpc/public-private-igw?ref=v16.1.0"

  name = "storage-172-30-0-0-16"

  cidr_block_vpc            = "172.30.0.0/16"
  cidr_block_public         = "172.30.0.0/17"
  cidrsubnet_newbits_public = "2"

  cidr_block_private         = "172.30.128.0/17"
  cidrsubnet_newbits_private = "2"
}
