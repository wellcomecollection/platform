ROOT = $(shell git rev-parse --show-toplevel)
INFRA_BUCKET = wellcomecollection-platform-infra

# Publish a ZIP file containing a Lambda definition to S3.
#
# Args:
#   $1 - Path to the Lambda src directory, relative to the root of the repo.
#
define publish_lambda
    $(ROOT)/docker_run.py --aws --root -- \
        wellcome/publish_lambda:14 \
        "$(1)" --key="lambdas/$(1).zip" --bucket="$(INFRA_BUCKET)" --sns-topic="arn:aws:sns:eu-west-1:760097843905:lambda_pushes"
endef


# Test a Python project.
#
# Args:
#   $1 - Path to the Python project's directory, relative to the root
#        of the repo.
#
define test_python
	$(ROOT)/docker_run.py --aws --dind -- \
		wellcome/build_test_python:55 $(1)

	$(ROOT)/docker_run.py --aws --dind -- \
		--net=host \
		--volume $(ROOT)/shared_conftest.py:/conftest.py \
		--workdir $(ROOT)/$(1) --tty \
		wellcome/test_python_$(shell basename $(1)):latest
endef


# Build and tag a Docker image.
#
# Args:
#   $1 - Name of the image.
#   $2 - Path to the Dockerfile, relative to the root of the repo.
#
define build_image
	$(ROOT)/docker_run.py \
	    --dind -- \
	    wellcome/image_builder:23 \
            --project=$(1) \
            --file=$(2)
endef


# Publish a Docker image to ECR, and put its associated release ID in S3.
#
# Args:
#   $1 - Name of the Docker image.
#
define publish_service
	$(ROOT)/docker_run.py \
	    --aws --dind -- \
	    wellcome/publish_service:30 \
	        --project="$(1)" \
	        --namespace=uk.ac.wellcome \
	        --infra-bucket="$(INFRA_BUCKET)" \
			--sns-topic="arn:aws:sns:eu-west-1:760097843905:ecr_pushes"
endef


# Publish a Docker image to ECR, and put its associated release ID in S3.
#
# Args:
#   $1 - Name of the Docker image
#   $2 - Stack name
#   $3 - ECR Repository URI
#   $4 - Registry ID
#
define publish_service_ssm
	$(ROOT)/docker_run.py \
	    --aws --dind -- \
	    wellcome/publish_service:51 \
	        --project_name=$(2) \
	        --registry_id=$(4) \
	        --label=latest \
	        --image_name="$(1)" \
	        --repo_uri="$(3)" \

endef


# Define a series of Make tasks (test, publish) for a Python Lambda.
#
# Args:
#	$1 - Name of the target.
#	$2 - Path to the Lambda source directory.
#
define __lambda_target_template
$(1)-test:
	$(call test_python,$(2))

$(1)-publish:
	$(call publish_lambda,$(2))

$(ROOT)/$(2)/src/requirements.txt: $(ROOT)/$(2)/src/requirements.in
	$(ROOT)/docker_run.py -- \
		--volume $(ROOT)/$(2)/src:/src micktwomey/pip-tools

$(ROOT)/$(2)/src/test_requirements.txt: $(ROOT)/$(2)/src/test_requirements.in
	$(ROOT)/docker_run.py -- \
		--volume $(ROOT)/$(2)/src:/src micktwomey/pip-tools \
		pip-compile test_requirements.in
endef


# Define a series of Make tasks (build, test, publish) for an ECS service.
#
# Args:
#	$1 - Name of the ECS service.
#	$2 - Path to the associated Dockerfile.
#	$3 - Stack name
#   $4 - ECS Base URI
#   $5 - Registry ID
#
define __python_target
$(1)-build:
	$(call build_image,$(1),$(2))

$(1)-test:
	$(call test_python,$(ROOT)/$(1))
endef

define __python_ssm_target
$(1)-build:
	$(call build_image,$(1),$(2))

$(1)-test:
	$(call test_python,$(ROOT)/$(1))

$(1)-publish: $(1)-build
	$(call publish_service_ssm,$(1),$(3),$(4),$(5))
endef


# Define all the Make tasks for a stack.
#
# Args:
#
#	$STACK_ROOT             Path to this stack, relative to the repo root
#
#	$PYTHON_APPS              A space delimited list of ECS services
#	$LAMBDAS                A space delimited list of Lambdas in this stack
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

$(foreach task,$(PYTHON_APPS),$(eval $(call __python_target,$(task),$(STACK_ROOT)/$(task)/Dockerfile)))
$(foreach task,$(PYTHON_SSM_APPS),$(eval $(call __python_ssm_target,$(task),$(STACK_ROOT)/$(task)/Dockerfile,$(STACK_ROOT),$(ECR_BASE_URI),$(REGISTRY_ID))))
$(foreach lamb,$(LAMBDAS),$(eval $(call __lambda_target_template,$(lamb),$(STACK_ROOT)/$(lamb))))
endef
