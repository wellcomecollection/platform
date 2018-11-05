ROOT = $(shell git rev-parse --show-toplevel)
MAKEFILES = $(ROOT)/makefiles

include $(MAKEFILES)/git.Makefile


# Define a series of Make tasks (plan, apply) for a Terraform stack.
#
# Args:
#	$1 - Name of the stack.
#	$2 - Root to the Terraform directory.
#	$3 - Is this a public-facing stack?  (true/false)
#
define terraform_target_template
$(1)-terraform-plan: uptodate-git
	$(ROOT)/docker_run.py --aws -- \
		--volume $(ROOT):$(ROOT) \
		--workdir $(ROOT)/$(2) \
		--env OP=plan \
		--env GET_TFVARS=true \
		--env BUCKET_NAME=$(WELLCOME_INFRA_BUCKET) \
		--env OBJECT_KEY=terraform.tfvars \
		--env IS_PUBLIC_FACING=$(3) \
		$(DOCKER_IMG_TERRAFORM_WRAPPER)


	$(call terraform_plan,$(2),$(3))

$(1)-terraform-apply: uptodate-git
	$(ROOT)/docker_run.py --aws -- \
		--volume $(ROOT):$(ROOT) \
		--workdir $(ROOT)/$(2) \
		--env BUCKET_NAME=$(WELLCOME_MONITORING_BUCKET) \
		--env OP=apply \
		$(DOCKER_IMG_TERRAFORM_WRAPPER)

# These are a pair of dodgy hacks to allow us to run something like:
#
#	$ make stack-terraform-import aws_s3_bucket.bucket my-bucket-name
#
#	$ make stack-terraform-state-rm aws_s3_bucket.bucket
#
# In practice it slightly breaks the conventions of Make (you're not meant to
# read command-line arguments), but since this is only for one-offs I think
# it's okay.
#
# This is slightly easier than using terraform on the command line, as paths
# are different in/outside Docker, so you have to reload all your modules,
# which is slow and boring.
#
$(1)-terraform-import:
	$(ROOT)/docker_run.py --aws -- \
		--volume $(ROOT):$(ROOT) \
		--workdir $(ROOT)/$(2) \
		$(DOCKER_IMG_TERRAFORM) import $(filter-out $(1)-terraform-import,$(MAKECMDGOALS))

$(1)-terraform-state-rm:
	$(ROOT)/docker_run.py --aws -- \
		--volume $(ROOT):$(ROOT) \
		--workdir $(ROOT)/$(2) \
		$(DOCKER_IMG_TERRAFORM) state rm $(filter-out $(1)-terraform-state-rm,$(MAKECMDGOALS))

endef
