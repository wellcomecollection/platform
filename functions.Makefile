ROOT = $(shell git rev-parse --show-toplevel)
INFRA_BUCKET = platform-infra


ifndef UPTODATE_GIT_DEFINED

# This target checks if you're up-to-date with the current master.
# This avoids problems where Terraform goes backwards or breaks
# already-applied changes.
#
# Consider the following scenario:
#
#     * --- * --- X --- Z                 master
#                  \
#                   Y --- Y --- Y         feature branch
#
# We cut a feature branch at X, then applied commits Y.  Meanwhile master
# added commit Z, and ran `terraform apply`.  If we run `terraform apply` on
# the feature branch, this would revert the changes in Z!  We'd rather the
# branches looked like this:
#
#     * --- * --- X --- Z                 master
#                        \
#                         Y --- Y --- Y   feature branch
#
# So that the commits in the feature branch don't unintentionally revert Z.
#
uptodate-git:
	@git fetch origin
	@if ! git merge-base --is-ancestor origin/master HEAD; then \
		echo "You need to be up-to-date with master before you can continue!"; \
		exit 1; \
	fi

UPTODATE_GIT_DEFINED = true

endif


# Run a 'terraform plan' step.
#
# Args:
#   $1 - Path to the Terraform directory.
#	$2 - true/false: is this a public-facing stack?
#
define terraform_plan
	make uptodate-git
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT):/data \
		--workdir /data/$(1) \
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
		--volume $(ROOT):/data \
		--workdir /data/$(1) \
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


# Test a Lambda project.
#
# Args:
#   $1 - Path to the Lambda directory, relative to the root of the repo.
#
define test_lambda
	$(ROOT)/builds/docker_run.py --aws --dind -- \
		--volume $(ROOT):/repo \
		wellcome/build_test_lambda $(1)

	$(ROOT)/builds/docker_run.py --aws --dind -- \
		--net=host \
		--volume $(ROOT)/$(1)/src:/data \
		--volume $(ROOT)/lambda_conftest.py:/conftest.py \
		--env INSTALL_DEPENDENCIES=false \
		--env FIND_MATCH_PATHS="/data" --tty \
		wellcome/test_lambda_$(shell basename $(1)):latest
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


# Define a series of Make tasks (build, test, publish) for a Scala application.
#
# Args:
#	$1 - Name of the project in sbt.
#	$2 - Root of the project's source code.
#
define __sbt_target_template
$(1)-build:
	$(call sbt_build,$(1))
	$(call build_image,$(1),$(2)/Dockerfile)

$(1)-test:
	$(call sbt_test,$(1))

$(1)-publish: $(1)-build
	$(call publish_service,$(1))
endef


# Define a series of Make tasks (plan, apply) for a Terraform stack.
#
# Args:
#	$1 - Name of the stack.
#	$2 - Root to the Terraform directory.
#	$3 - Is this a public-facing stack?  (true/false)
#
define __terraform_target_template
$(1)-terraform-plan:
	$(call terraform_plan,$(2),$(3))

$(1)-terraform-apply:
	$(call terraform_apply,$(2))
endef


# Define a series of Make tasks (test, publish) for a Python Lambda.
#
# Args:
#	$1 - Name of the target.
#	$2 - Path to the Lambda source directory.
#
define __lambda_target_template
$(1)-test: 
	$(call test_lambda,$(2))

$(1)-publish:
	$(call publish_lambda,$(2))

$(ROOT)/$(2)/src/requirements.txt:
	$(ROOT)/builds/docker_run.py -- \
		--volume $(ROOT)/$(2)/src:/src micktwomey/pip-tools

$(ROOT)/$(2)/src/test_requirements.txt:
	$(ROOT)/builds/docker_run.py -- \
		--volume $(ROOT)/$(2)/src:/src micktwomey/pip-tools \
		pip-compile test_requirements.in
endef


# Define a series of Make tasks (build, test, publish) for an ECS service.
#
# Args:
#	$1 - Name of the ECS service.
#	$2 - Path to the associated Dockerfile.
#
define __ecs_target_template
$(1)-build:
	$(call build_image,$(1),$(2))

$(1)-publish: $(1)-build
	$(call publish_service,$(1))
endef


# Define all the Make tasks for a stack.
#
# Args:
#
#	$STACK_ROOT             Path to this stack, relative to the repo root
#
#	$SBT_APPS               A space delimited list of sbt apps in this stack
#	$ECS_TASKS              A space delimited list of ECS services
#	$LAMBDAS                A space delimited list of Lambdas in this stack
#
#	$TF_NAME                Name of the associated Terraform stack
#	$TF_PATH                Path to the associated Terraform stack
#	$TF_IS_PUBLIC_FACING    Is this a public-facing stack?  (true/false)
#
define stack_setup

# The structure of each of these lines is as follows:
#
#	$(foreach name,$(NAMES),
#		$(eval
#			$(call __target_template,$(arg1),...,$(argN))
#		)
#	)
#
# It can't actually be written that way because Make is very sensitive to
# whitespace, but that's the general idea.

$(foreach proj,$(SBT_APPS),$(eval $(call __sbt_target_template,$(proj),$(STACK_ROOT)/$(proj))))
$(foreach task,$(ECS_TASKS),$(eval $(call __ecs_target_template,$(task),$(STACK_ROOT)/$(task)/Dockerfile)))
$(foreach lamb,$(LAMBDAS),$(eval $(call __lambda_target_template,$(lamb),$(STACK_ROOT)/$(lamb))))
$(foreach name,$(TF_NAME),$(eval $(call __terraform_target_template,$(TF_NAME),$(TF_PATH),$(TF_IS_PUBLIC_FACING))))
endef
