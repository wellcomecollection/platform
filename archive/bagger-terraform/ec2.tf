module "bnumbers_processing_ec2" {
  source = "git::https://github.com/wellcometrust/terraform.git//ec2/prebuilt/ebs?ref=v11.6.1"

  name     = "bagger-bnumbers-processing"
  asg_max  = "1"
  key_name = "${var.ec2_key_name}"
  image_id = "${var.ec2_image_id}"

  vpc_id      = "${module.network.vpc_id}"
  subnet_list = ["${module.network.public_subnets}"]

  controlled_access_cidr_ingress = [
    "195.143.129.132/32",
    "46.102.195.182/32",
  ]

  ebs_size = "100"

  user_data = <<EOF
#!/bin/bash

yum update -q -y
yum install -q -y jq aws-cli amazon-efs-utils git
amazon-linux-extras install python3

# mount data volume
mkdir -p /data
echo "/dev/xvdb      /data   ext4    defaults,nofail  0 2" >> /etc/fstab
echo "mounting data volume"
mount -a -v

git clone https://github.com/wellcometrust/platform /opt/platform
cd /opt/platform
git checkout archive-bagger

pip3 install -r /opt/platform/archive/bagger/requirements.txt

cat > /opt/


EOF
}

variable "bnumbers_processing_mets_filesystem_root" {}
variable "bnumbers_processing_mets_bucket_name" {}
variable "bnumbers_processing_read_mets_from_fileshare" {}
variable "bnumbers_processing_working_directory" {}
variable "bnumbers_processing_drop_bucket_name" {}
variable "bnumbers_processing_drop_bucket_name_mets_only" {}
variable "bnumbers_processing_current_preservation_bucket" {}
variable "bnumbers_processing_dlcs_source_bucket" {}
variable "bnumbers_processing_dlcs_entry" {}
variable "bnumbers_processing_dlcs_api_key" {}
variable "bnumbers_processing_dlcs_api_secret" {}
variable "bnumbers_processing_dlcs_customer_id" {}
variable "bnumbers_processing_dlcs_space" {}
variable "bnumbers_processing_dds_api_key" {}
variable "bnumbers_processing_dds_api_secret" {}
variable "bnumbers_processing_dds_asset_prefix" {}
