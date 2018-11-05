ROOT = $(shell git rev-parse --show-toplevel)
INFRA_BUCKET = wellcomecollection-platform-infra

WELLCOME_INFRA_BUCKET      = wellcomecollection-platform-infra
WELLCOME_MONITORING_BUCKET = wellcomecollection-platform-monitoring

LAMBDA_PUSHES_TOPIC_ARN = arn:aws:sns:eu-west-1:760097843905:lambda_pushes
ECR_PUSHES_TOPIC_ARN    = "arn:aws:sns:eu-west-1:760097843905:ecr_pushes"

DOCKER_IMG_BUILD_TEST_PYTHON = wellcome/build_test_python
DOCKER_IMG_PUBLISH_LAMBDA    = wellcome/publish_lambda:12
DOCKER_IMG_TERRAFORM         = hashicorp/terraform:0.11.10
DOCKER_IMG_TERRAFORM_WRAPPER = wellcome/terraform_wrapper:13
DOCKER_IMG_IMAGE_BUILDER     = wellcome/image_builder:latest
DOCKER_IMG_PUBLISH_SERVICE   = wellcome/publish_service:latest
DOCKER_IMG_SBT_WRAPPER       = wellcome/sbt_wrapper

include makefiles/docker.Makefile
include makefiles/python.Makefile
include makefiles/sbt.Makefile
include makefiles/terraform.Makefile


# Define a series of Make tasks (build, test, publish) for an ECS service.
#
# Args:
#	$1 - Name of the ECS service.
#	$2 - Path to the associated Dockerfile.
#
define __ecs_target_template
$(1)-build:
	$(call build_image,$(1),$(2))

$(1)-test:
	$(call test_python,$(STACK_ROOT)/$(1))

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
#	$SBT_DOCKER_LIBRARIES   A space delimited list of sbt libraries  in this stack that use docker compose for tests
#	$SBT_NO_DOCKER_LIBRARIES   A space delimited list of sbt libraries  in this stack that use docker compose for tests
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

$(foreach proj,$(SBT_APPS),$(eval $(call sbt_target_template,$(proj),$(STACK_ROOT)/$(proj))))
$(foreach library,$(SBT_DOCKER_LIBRARIES),$(eval $(call sbt_library_docker_template,$(library),$(STACK_ROOT)/$(library))))
$(foreach library,$(SBT_NO_DOCKER_LIBRARIES),$(eval $(call sbt_library_template,$(library))))
$(foreach task,$(ECS_TASKS),$(eval $(call __ecs_target_template,$(task),$(STACK_ROOT)/$(task)/Dockerfile)))
$(foreach lamb,$(LAMBDAS),$(eval $(call lambda_target_template,$(lamb),$(STACK_ROOT)/$(lamb))))
$(foreach name,$(TF_NAME),$(eval $(call terraform_target_template,$(TF_NAME),$(TF_PATH),$(TF_IS_PUBLIC_FACING))))
endef
