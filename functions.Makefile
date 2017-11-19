ROOT = $(shell git rev-parse --show-toplevel)
INFRA_BUCKET = platform-infra

# Run a 'terraform plan' step.
#
# Args:
#   $1 - Path to the Terraform directory.
#
define terraform_plan
	make uptodate-git
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(1):/data \
		--env OP=plan \
		wellcome/terraform_wrapper:latest
endef


# Run a 'terraform apply' step.
#
# Args:
#   $1 - Path to the Terraform directory.
#
define terraform_apply
	make uptodate-git
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(1):/data \
		--env OP=apply \
		wellcome/terraform_wrapper:latest
endef


# Publish a ZIP file containing a Lambda definition to S3.
#
# Args:
#   $1 - Path to the Lambda source, relative to the root of the repo.
#
define publish_lambda
	make $(ROOT)/.docker/publish_lambda_zip
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT):/repo \
		wellcome/publish_lambda:latest \
		"$(1)/src" --key="lambdas/$(1).zip" --bucket="$(INFRA_BUCKET)"
endef


# Build and tag a Docker image.
#
# Args:
#   $1 - Name of the image.
#   $2 - Path to the Dockerfile, relative to the root of the repo.
#
define build_image
	$(ROOT)/builds/docker_run.py \
	    --dind -- \
	    wellcome/image_builder:latest \
            --project=$(1) \
            --file=$(2)
endef


# Build and tag a Docker image (specifically for ngnix).
#
# Args:
#   $1 - Name of the variant.
#
define nginx_build_image
	$(ROOT)/builds/docker_run.py \
		--dind -- \
		wellcome/image_builder:latest \
            --project=nginx \
            --variant=$(1)
endef


# Publish a Docker image to ECR, and put its associated release ID in S3.
#
# Args:
#   $1 - Name of the Docker image.
#
define publish_service
	make $(ROOT)/.docker/publish_service_to_aws
	$(ROOT)/builds/docker_run.py --aws --dind -- \
		publish_service_to_aws --project="$(1)" --infra-bucket="$(INFRA_BUCKET)"
endef

