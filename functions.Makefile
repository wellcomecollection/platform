ROOT = $(shell git rev-parse --show-toplevel)


# Run a 'terraform plan' step.
#
# Args:
#   1 - Path to the Terraform directory.
#
define terraform_plan
	make uptodate-git
	make $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(1):/data \
		--env OP=plan \
		terraform_ci:latest
endef


# Run a 'terraform apply' step.
#
# Args:
#   1 - Path to the Terraform directory.
#
define terraform_apply
	make uptodate-git
	make $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(1):/data \
		--env OP=apply \
		terraform_ci:latest
endef


# Publish a ZIP file containing a Lambda definition to S3.
#
# Args:
#   1 - Path to the Lambda source, relative to the root of the repo.
#
define publish_lambda
	make $(ROOT)/.docker/publish_lambda_zip
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT):/repo \
		publish_lambda_zip \
		"$(1)/$(2)/src" --key="lambdas/$(1)/$(2).zip" --bucket="$(INFRA_BUCKET)"
endef
