ROOT = $(shell git rev-parse --show-toplevel)
INFRA_BUCKET = platform-infra

SIERRA_ADAPTER = $(ROOT)/sierra_adapter

include $(SIERRA_ADAPTER)/sierra_objects_to_s3/Makefile
include $(SIERRA_ADAPTER)/sierra_window_generator/Makefile

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile


sierra_to_dynamo-test: $(ROOT)/.docker/sbt_test
	PROJECT=sierra_to_dynamo $(ROOT)/builds/test_sbt_project.sh

sierra_to_dynamo-build: $(ROOT)/.docker/sbt_test
	PROJECT=sierra_to_dynamo $(ROOT)./builds/run_sbt_image_build.sh

sierra_to_dynamo-publish: sierra_to_dynamo-build
	$(call publish_service,sierra_to_dynamo)

sierra_to_dynamo-publish:
	echo "Hello world"

sierra_adapter-test: sierra_to_dynamo-test sierra_window_generator-test
sierra_adapter-publish: sierra_to_dynamo-publish sierra_window_generator-publish

sierra_adapter-publish:
	echo "Nothing to do!"

sierra_adapter-terraform-plan:
	$(call terraform_plan,$(SIERRA_ADAPTER))

sierra_adapter-terraform-apply:
	$(call terraform_apply,$(SIERRA_ADAPTER))


.PHONY: sierra_adapter-terraform-plan sierra_adapter-terraform-apply
