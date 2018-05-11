ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= sbt_common

SBT_DOCKER_LIBRARIES = common_elasticsearch common_messaging common_monitoring common_storage

$(foreach library,$(SBT_DOCKER_LIBRARIES),$(eval $(call __sbt_library_docker_template,$(library),$(STACK_ROOT)/$(library))))

sbt-internal_model-test:
	$(call sbt_test_no_docker,internal_model)

sbt-internal_model-publish:
	echo "Nothing to do!"

sbt-display-test:
	$(call sbt_test_no_docker,common_display)

sbt-display-publish:
	echo "Nothing to do!"
