include makefiles/docker.Makefile
include makefiles/python.Makefile
include makefiles/sbt.Makefile
include makefiles/terraform.Makefile


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
$(foreach task,$(ECS_TASKS),$(eval $(call python_ecs_target_template,$(task),$(STACK_ROOT)/$(task)/Dockerfile)))
$(foreach lamb,$(LAMBDAS),$(eval $(call lambda_target_template,$(lamb),$(STACK_ROOT)/$(lamb))))
$(foreach name,$(TF_NAME),$(eval $(call terraform_target_template,$(TF_NAME),$(TF_PATH),$(TF_IS_PUBLIC_FACING))))
endef
