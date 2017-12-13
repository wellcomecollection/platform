include shared.Makefile
include functions.Makefile

include loris/Makefile
include shared_infra/Makefile
include catalogue_api/Makefile
include catalogue_pipeline/Makefile
include miro_preprocessor/Makefile
include monitoring/Makefile
include ontologies/Makefile
include sierra_adapter/Makefile
include nginx/Makefile


sbt-common-test:
	$(call sbt_test,common)

sbt-common-publish:
	echo "Nothing to do!"

$(ROOT)/.docker/sbt_image_builder:
	./builds/build_ci_docker_image.py \
		--project=sbt_image_builder \
		--dir=builds \
		--file=builds/sbt_image_builder.Dockerfile

format: format-terraform format-scala

check-format: format lint-python lint-ontologies
	git diff --exit-code

travis-format:
	python3 .travis/run_autoformat.py
