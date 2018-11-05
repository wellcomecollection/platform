# Test an sbt project.
#
# Args:
#   $1 - Name of the project.
#
define sbt_test
	$(ROOT)/docker_run.py --dind --sbt --root -- \
		--net host \
		$(DOCKER_IMG_SBT_WRAPPER) \
		"project $(1)" ";dockerComposeUp;test;dockerComposeStop"
endef



# Test an sbt project without docker-compose.
#
# Args:
#   $1 - Name of the project.
#
define sbt_test_no_docker
	$(ROOT)/docker_run.py --dind --sbt --root -- \
		--net host \
		$(DOCKER_IMG_SBT_WRAPPER) \
		"project $(1)" "test"
endef


# Build an sbt project.
#
# Args:
#   $1 - Name of the project.
#
define sbt_build
	$(ROOT)/docker_run.py --dind --sbt --root -- \
		--net host \
		$(DOCKER_IMG_SBT_WRAPPER) \
		"project $(1)" ";stage"
endef


# Run docker-compose up.
#
# Args:
#   $1 - Path to the docker-compose file.
#
define docker_compose_up
	$(ROOT)/docker_run.py --dind --sbt --root -- \
		--net host \
		$(DOCKER_IMG_SBT_WRAPPER) \
		"project $(1)" "dockerComposeUp"
endef


# Run docker-compose down.
#
# Args:
#   $1 - Path to the docker-compose file.
#
define docker_compose_down
	$(ROOT)/docker_run.py --dind --sbt --root -- \
		--net host \
		$(DOCKER_IMG_SBT_WRAPPER) \
		"project $(1)" "dockerComposeDown"
endef


# Define a series of Make tasks for a Scala modules that use docker-compose for tests.
#
# Args:
#	$1 - Name of the project in sbt.
#	$2 - Root of the project's source code.
#
define __sbt_base_docker_template
$(1)-docker_compose_up:
	$(call docker_compose_up,$(1))

$(1)-docker_compose_down:
	$(call docker_compose_down,$(1))

$(1)-test:
	$(call sbt_test,$(1))

endef


# Define a series of Make tasks (build, test, publish) for a Scala services.
#
# Args:
#	$1 - Name of the project in sbt.
#	$2 - Root of the project's source code.
#
define sbt_target_template
$(eval $(call __sbt_base_docker_template,$(1),$(2)))

$(1)-build:
	$(call sbt_build,$(1))
	$(call build_image,$(1),$(2)/Dockerfile)

$(1)-publish: $(1)-build
	$(call publish_service,$(1))
endef


# Define a series of Make tasks for a Scala libraries that use docker-compose for tests.
#
# Args:
#	$1 - Name of the project in sbt.
#	$2 - Root of the project's source code.
#
define sbt_library_docker_template
$(eval $(call __sbt_base_docker_template,$(1),$(2)))

$(1)-publish:
	echo "Nothing to do!"

endef


# Define a series of Make tasks for a Scala library that doesn't use Docker.
#
# Args:
#	$1 - Name of the project in sbt.
#	$2 - Root of the project's source code.
#
define sbt_library_template
$(1)-test:
	$(call sbt_test_no_docker,$(1))

$(1)-publish:
	echo "Nothing to do!"

endef
