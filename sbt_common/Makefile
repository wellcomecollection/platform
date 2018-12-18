ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= sbt_common

SBT_DOCKER_LIBRARIES = elasticsearch monitoring storage
SBT_NO_DOCKER_LIBRARIES = common internal_model display

$(foreach library,$(SBT_DOCKER_LIBRARIES),$(eval $(call __sbt_library_docker_template,$(library),$(STACK_ROOT)/$(library))))
$(foreach library,$(SBT_NO_DOCKER_LIBRARIES),$(eval $(call __sbt_library_template,$(library))))
