ROOT = $(shell git rev-parse --show-toplevel)
INFRA_BUCKET = wellcomecollection-platform-infra

WELLCOME_INFRA_BUCKET      = wellcomecollection-platform-infra
WELLCOME_MONITORING_BUCKET = wellcomecollection-platform-monitoring

LAMBDA_PUSHES_TOPIC_ARN = arn:aws:sns:eu-west-1:760097843905:lambda_pushes
ECR_PUSHES_TOPIC_ARN    = "arn:aws:sns:eu-west-1:760097843905:ecr_pushes"

DOCKER_IMG_BUILD_TEST_PYTHON = wellcome/build_test_python
DOCKER_IMG_PUBLISH_LAMBDA    = wellcome/publish_lambda:12
DOCKER_IMG_TERRAFORM         = hashicorp/terraform:0.11.10
DOCKER_IMG_TERRAFORM_WRAPPER = wellcome/terraform_wrapper:13
DOCKER_IMG_IMAGE_BUILDER     = wellcome/image_builder:latest
DOCKER_IMG_PUBLISH_SERVICE   = wellcome/publish_service:latest
DOCKER_IMG_SBT_WRAPPER       = wellcome/sbt_wrapper


include functions.Makefile

include formatting.Makefile

include archive/Makefile
include assets/Makefile
include builds/Makefile
include catalogue_api/Makefile
include catalogue_pipeline/Makefile
include data_api/Makefile
include data_science/Makefile
include goobi_adapter/Makefile
include infra_critical/Makefile
include loris/Makefile
include monitoring/Makefile
include nginx/Makefile
include ontologies/Makefile
include reindexer/Makefile
include reporting/Makefile
include sbt_common/Makefile
include shared_infra/Makefile
include sierra_adapter/Makefile


travis-lambda-test:
	python run_travis_lambdas.py test

travis-lambda-publish:
	python run_travis_lambdas.py publish


travistooling-test:
	$(ROOT)/docker_run.py -- \
		--volume $(ROOT):/data \
		--env encrypted_83630750896a_key=$(encrypted_83630750896a_key) \
		--env encrypted_83630750896a_iv=$(encrypted_83630750896a_iv) \
		wellcome/build_tooling \
		coverage run --rcfile=travistooling/.coveragerc --module py.test travistooling/tests/test_*.py
	$(ROOT)/docker_run.py -- \
		--volume $(ROOT):/data \
		wellcome/build_tooling \
		coverage report --rcfile=travistooling/.coveragerc

travistooling-publish:
	$(error "Nothing to do for this task")
