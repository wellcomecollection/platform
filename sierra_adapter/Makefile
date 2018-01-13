ROOT = $(shell git rev-parse --show-toplevel)
SIERRA_ADAPTER = $(ROOT)/sierra_adapter

include $(SIERRA_ADAPTER)/sierra_window_generator/Makefile
include $(ROOT)/functions.Makefile


sierra_common-test:
	$(call sbt_test,sierra_adapter_common)

sierra_common-publish:
	echo "Nothing to do!"


sierra_reader-test:
	$(call sbt_test,sierra_reader)

sierra_reader-build:
	$(call sbt_build,sierra_reader)
	$(call build_image,sierra_reader,sierra_adapter/sierra_reader/Dockerfile)

sierra_reader-publish: sierra_reader-build
	$(call publish_service,sierra_reader)


sierra_items_to_dynamo-test:
	$(call sbt_test,sierra_items_to_dynamo)

sierra_items_to_dynamo-build:
	$(call sbt_build,sierra_items_to_dynamo)
	$(call build_image,sierra_items_to_dynamo,sierra_adapter/sierra_items_to_dynamo/Dockerfile)

sierra_items_to_dynamo-publish: sierra_items_to_dynamo-build
	$(call publish_service,sierra_items_to_dynamo)


sierra_bib_merger-test:
	$(call sbt_test,sierra_bib_merger)

sierra_bib_merger-build:
	$(call sbt_build,sierra_bib_merger)
	$(call build_image,sierra_bib_merger,sierra_adapter/sierra_bib_merger/Dockerfile)

sierra_bib_merger-publish: sierra_bib_merger-build
	$(call publish_service,sierra_bib_merger)


sierra_item_merger-test:
	$(call sbt_test,sierra_item_merger)

sierra_item_merger-build:
	$(call sbt_build,sierra_item_merger)
	$(call build_image,sierra_item_merger,sierra_adapter/sierra_item_merger/Dockerfile)

sierra_item_merger-publish: sierra_item_merger-build
	$(call publish_service,sierra_item_merger)


s3_demultiplexer-test:
	$(call test_lambda,sierra_adapter/s3_demultiplexer)

s3_demultiplexer-publish:
	$(call publish_lambda,sierra_adapter/s3_demultiplexer)


sierra_adapter-terraform-plan:
	$(call terraform_plan,$(SIERRA_ADAPTER)/terraform,false)

sierra_adapter-terraform-apply:
	$(call terraform_apply,$(SIERRA_ADAPTER)/terraform)
