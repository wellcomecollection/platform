TERRAFORM_IMAGE = hashicorp/terraform:0.11.11


define _terraform_run
	$(ROOT)/docker_run.py --aws --\
		--volume $(ROOT):$(ROOT) \
		--workdir $(ROOT)/$(1) \
		$(TERRAFORM_IMAGE)
endef


# Run a 'terraform plan' step.
#
# Args:
#   $1 - Path to the Terraform directory.
#
define terraform_plan
	$(_terraform_run) init
	$(_terraform_run) get
	$(_terraform_run) plan -out terraform.plan
endef


# Run a 'terraform apply' step.
#
# Args:
#   $1 - Path to the Terraform directory.
#
define terraform_apply
	$(_terraform_run) apply terraform.plan
endef



# Define a series of Make tasks (plan, apply) for a Terraform stack.
#
# Args:
#	$1 - Name of the stack.
#	$2 - Root to the Terraform directory.
#
define __terraform_target_template
$(1)-terraform-plan:
	$(call terraform_plan,$(2))

$(1)-terraform-apply:
	$(call terraform_apply,$(2))

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
		$(TERRAFORM_IMAGE) import $(filter-out $(1)-terraform-import,$(MAKECMDGOALS))

$(1)-terraform-state-rm:
	$(ROOT)/docker_run.py --aws -- \
		--volume $(ROOT):$(ROOT) \
		--workdir $(ROOT)/$(2) \
		$(TERRAFORM_IMAGE) state rm $(filter-out $(1)-terraform-state-rm,$(MAKECMDGOALS))

endef