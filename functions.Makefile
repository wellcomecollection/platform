ROOT = $(shell git rev-parse --show-toplevel)
INFRA_BUCKET = platform-infra

# Run a 'terraform plan' step.
#
# Args:
#   $1 - Path to the Terraform directory.
#	$2 - true/false: is this a public-facing stack?
#
define terraform_plan
	make uptodate-git
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(1):/data \
		--env OP=plan \
		--env GET_PLATFORM_TFVARS=true \
		--env IS_PUBLIC_FACING=$(2) \
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
#   $1 - Path to the Lambda src directory, relative to the root of the repo.
#
define publish_lambda
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT):/repo \
		wellcome/publish_lambda:latest \
		"$(1)/src" --key="lambdas/$(1).zip" --bucket="$(INFRA_BUCKET)"
endef


# Build a Lambda project.
#
# Args:
#   $1 - Path to the Lambda src directory, relative to the root of the repo.
#
define build_lambda
	make  $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py -- \
		--volume $(ROOT)/$(1):/data \
		--env OP=build-lambda \
		python3.6_ci:latest
endef


# Test a Lambda project.
#
# Args:
#   $1 - Path to the Lambda directory, relative to the root of the repo.
#
define test_lambda
	make $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT)/$(1)/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest
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


# Publish a Docker image to ECR, and put its associated release ID in S3.
#
# Args:
#   $1 - Name of the Docker image.
#
define publish_service
	$(ROOT)/builds/docker_run.py \
	    --aws --dind -- \
	    wellcome/publish_service:latest \
	        --project="$(1)" \
	        --namespace=uk.ac.wellcome \
	        --infra-bucket="$(INFRA_BUCKET)"
endef


# Test an sbt project.
#
# Args:
#   $1 - Name of the project.
#
define sbt_test
	$(ROOT)/builds/docker_run.py --dind --sbt --root -- \
		--net host \
		wellcome/sbt_wrapper \
		"project $(1)" ";dockerComposeUp;test;dockerComposeStop"
endef


# Build an sbt project.
#
# Args:
#   $1 - Name of the project.
#
define sbt_build
	$(ROOT)/builds/docker_run.py --dind --sbt --root -- \
		--net host \
		wellcome/sbt_wrapper \
		"project $(1)" ";stage"
endef
