include shared.Makefile
include loris/Makefile
include shared_infra/Makefile
include catalogue_api/Makefile
include catalogue_pipeline/Makefile
include miro_preprocessor/Makefile
include monitoring/Makefile
include ontologies/Makefile
include sierra_adapter/Makefile
include nginx/Makefile


api_docs-build:
	$(call build_image,update_api_docs,docker/update_api_docs/Dockerfile)

api_docs-deploy: api_docs-build
	$(call publish_service,update_api_docs)


.docker/sbt_test:
	./builds/build_ci_docker_image.py \
		--project=sbt_test \
		--dir=builds \
		--file=builds/sbt_test.Dockerfile

sbt-test-common: .docker/sbt_test
	PROJECT=common ./builds/test_sbt_project.sh

sbt-test-api: .docker/sbt_test
	PROJECT=api ./builds/test_sbt_project.sh

sbt-test-id_minter: .docker/sbt_test
	PROJECT=id_minter ./builds/test_sbt_project.sh

sbt-test-ingestor: .docker/sbt_test
	PROJECT=ingestor ./builds/test_sbt_project.sh

sbt-test-reindexer: .docker/sbt_test
	PROJECT=reindexer ./builds/test_sbt_project.sh

sbt-test-transformer: .docker/sbt_test
	PROJECT=transformer ./builds/test_sbt_project.sh


.docker/sbt_image_builder:
	./builds/build_ci_docker_image.py \
		--project=sbt_image_builder \
		--dir=builds \
		--file=builds/sbt_image_builder.Dockerfile

sbt-build-api: .docker/sbt_image_builder
	TARGET=catatlogue_api/api \
	PROJECT=api ./builds/run_sbt_image_build.sh

sbt-build-id_minter: .docker/sbt_image_builder
	TARGET=catalogue_pipeline/id_minter \
	PROJECT=id_minter ./builds/run_sbt_image_build.sh

sbt-build-ingestor: .docker/sbt_image_builder
	TARGET=catalogue_pipeline/ingestor \
	PROJECT=ingestor ./builds/run_sbt_image_build.sh

sbt-build-reindexer: .docker/sbt_image_builder
	TARGET=catalogue_pipeline/reindexer \
	PROJECT=reindexer ./builds/run_sbt_image_build.sh

sbt-build-transformer: .docker/sbt_image_builder
	TARGET=catalogue_pipeline/transformer \
	PROJECT=transformer ./builds/run_sbt_image_build.sh



sbt-deploy-api: sbt-build-api
	$(call publish_service,api)

sbt-deploy-id_minter: sbt-build-id_minter
	$(call publish_service,id_minter)

sbt-deploy-ingestor: sbt-build-ingestor
	$(call publish_service,ingestor)

sbt-deploy-reindexer: sbt-build-reindexer
	$(call publish_service,reindexer)

sbt-deploy-transformer: sbt-build-transformer
	$(call publish_service,transformer)

format: format-terraform format-scala

check-format: format lint-python lint-ontologies
	git diff --exit-code
