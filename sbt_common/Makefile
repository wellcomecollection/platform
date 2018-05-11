ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= sbt_common

SBT_DOCKER_LIBRARIES = common_elasticsearch common_messaging common_monitoring common_storage
SBT_NO_DOCKER_LIBRARIES = internal_model common_display

$(foreach library,$(SBT_NO_DOCKER_LIBRARIES),$(eval $(call __sbt_library_docker_template,$(library),$(STACK_ROOT)/$(library))))
$(foreach library,$(SBT_DOCKER_LIBRARIES),$(eval $(call __sbt_library_docker_template,$(library),$(STACK_ROOT)/$(library))))
