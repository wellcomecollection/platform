ROOT = $(shell git rev-parse --show-toplevel)
CATALOGUE_PIPELINE = $(ROOT)/catalogue_pipeline

include $(ROOT)/functions.Makefile
include $(CATALOGUE_PIPELINE)/schedule_reindexer/Makefile


id_minter-build:
	$(call sbt_build,id_minter)
	$(call build_image,id_minter,catalogue_pipeline/id_minter/Dockerfile)

ingestor-build:
	$(call sbt_build,ingestor)
	$(call build_image,ingestor,catalogue_pipeline/ingestor/Dockerfile)

reindexer-build:
	$(call sbt_build,reindexer)
	$(call build_image,reindexer,catalogue_pipeline/reindexer/Dockerfile)

transformer-build:
	$(call sbt_build,transformer)
	$(call build_image,transformer,catalogue_pipeline/transformer/Dockerfile)


id_minter-test:
	$(call sbt_test,id_minter)

ingestor-test:
	$(call sbt_test,ingestor)

reindexer-test:
	$(call sbt_test,reindexer)

transformer-test:
	$(call sbt_test,transformer)


id_minter-publish: id_minter-build
	$(call publish_service,id_minter)

ingestor-publish: ingestor-build
	$(call publish_service,ingestor)

reindexer-publish: reindexer-build
	$(call publish_service,reindexer)

transformer-publish: transformer-build
	$(call publish_service,transformer)


transformer_sns_filter-test:
	$(call test_lambda,catalogue_pipeline/transformer_sns_filter)

transformer_sns_filter-publish:
	$(call publish_lambda,catalogue_pipeline/transformer_sns_filter)



catalogue_pipeline-build: \
	schedule_reindexer-build \
	transformer_sns_filter-build \
	id_minter-build \
	ingestor-build \
	reindexer-build \
	transformer-build

catalogue_pipeline-test: \
	schedule_reindexer-test \
	transformer_sns_filter-test \
	id_minter-test \
	ingestor-test \
	reindexer-test \
	transformer-test

catalogue_pipeline-publish: \
	schedule_reindexer-publish \
	transformer_sns_filter-publish \
	id_minter-publish \
	ingestor-publish \
	reindexer-publish \
	transformer-publish


catalogue_pipeline-terraform-plan:
	$(call terraform_plan,$(CATALOGUE_PIPELINE)/terraform,false)

catalogue_pipeline-terraform-apply:
	$(call terraform_apply,$(CATALOGUE_PIPELINE)/terraform)
