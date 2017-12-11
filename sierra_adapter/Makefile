ROOT = $(shell git rev-parse --show-toplevel)
SIERRA_ADAPTER = $(ROOT)/sierra_adapter

include $(SIERRA_ADAPTER)/sierra_window_generator/Makefile
include $(ROOT)/functions.Makefile

sierra_to_dynamo-test:
	$(call sbt_test,sierra_to_dynamo)

sierra_to_dynamo-build: $(ROOT)/.docker/sbt_image_builder
	TARGET=sierra_adapter/sierra_to_dynamo/target \
	PROJECT=sierra_to_dynamo $(ROOT)/builds/run_sbt_image_build.sh

sierra_to_dynamo-publish: sierra_to_dynamo-build
	$(call publish_service,sierra_to_dynamo)


sierra_merger-test:
	$(call sbt_test,sierra_merger)

sierra_merger-build: $(ROOT)/.docker/sbt_image_builder
	TARGET=sierra_adapter/sierra_merger/target \
	PROJECT=sierra_merger $(ROOT)/builds/run_sbt_image_build.sh

sierra_merger-publish: sierra_merger-build
	$(call publish_service,sierra_merger)


sierra_adapter-test: \
	sierra_window_generator-test \
	sierra_to_dynamo-test \
	sierra_merger-test

sierra_adapter-publish: \
	sierra_window_generator-publish \
	sierra_to_dynamo-publish \
	sierra_merger-publish


sierra_adapter-terraform-plan:
	$(call terraform_plan,$(SIERRA_ADAPTER)/terraform)

sierra_adapter-terraform-apply:
	$(call terraform_apply,$(SIERRA_ADAPTER)/terraform)
