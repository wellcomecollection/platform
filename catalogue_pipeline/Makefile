ROOT = $(shell git rev-parse --show-toplevel)
CATALOGUE_PIPELINE = $(ROOT)/catalogue_pipeline

include $(ROOT)/functions.Makefile
include $(CATALOGUE_PIPELINE)/schedule_reindexer/Makefile


id_minter-build: $(ROOT)/.docker/sbt_image_builder
	TARGET=catalogue_pipeline/id_minter/target \
	PROJECT=id_minter $(ROOT)/builds/run_sbt_image_build.sh

ingestor-build: $(ROOT)/.docker/sbt_image_builder
	TARGET=catalogue_pipeline/ingestor/target \
	PROJECT=ingestor $(ROOT)/builds/run_sbt_image_build.sh

reindexer-build: $(ROOT)/.docker/sbt_image_builder
	TARGET=catalogue_pipeline/reindexer/target \
	PROJECT=reindexer $(ROOT)/builds/run_sbt_image_build.sh

transformer-build: $(ROOT)/.docker/sbt_image_builder
	TARGET=catalogue_pipeline/transformer/target \
	PROJECT=transformer $(ROOT)/builds/run_sbt_image_build.sh


id_minter-test: $(ROOT)/.docker/sbt_test
	PROJECT=id_minter $(ROOT)/builds/test_sbt_project.sh

ingestor-test: $(ROOT)/.docker/sbt_test
	PROJECT=ingestor $(ROOT)/builds/test_sbt_project.sh

reindexer-test: $(ROOT)/.docker/sbt_test
	PROJECT=reindexer $(ROOT)/builds/test_sbt_project.sh

transformer-test: $(ROOT)/.docker/sbt_test
	PROJECT=transformer $(ROOT)/builds/test_sbt_project.sh


id_minter-deploy: id_minter-build
	$(call publish_service,id_minter)

ingestor-deploy: ingestor-build
	$(call publish_service,ingestor)

reindexer-deploy: reindexer-build
	$(call publish_service,reindexer)

transformer-deploy: transformer-build
	$(call publish_service,transformer)


transformer_sns_filter-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(CATALOGUE_PIPELINE)/transformer_sns_filter:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

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
	id_minter-test \
	ingestor-test \
	reindexer-test \
	transformer-test

catalogue_pipeline-deploy: \
	schedule_reindexer-publish \
	transformer_sns_filter-publish \
	id_minter-deploy \
	ingestor-deploy \
	reindexer-deploy \
	transformer-deploy


catalogue_pipeline-terraform-plan:
	$(call terraform_plan,$(CATALOGUE_PIPELINE)/terraform)

catalogue_pipeline-terraform-apply:
	$(call terraform_apply,$(CATALOGUE_PIPELINE)/terraform)
