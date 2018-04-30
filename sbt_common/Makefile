ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

SBT_LIBS = \
	display \
	elasticsearch \
	internal-model

$(foreach lib_name,$(SBT_LIBS),$(eval $(call sbt_library_template,$(lib_name),sbt_common/$(lib_name))))
