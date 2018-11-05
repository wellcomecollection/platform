ROOT = $(shell git rev-parse --show-toplevel)

WELLCOME_INFRA_BUCKET = wellcomecollection-platform-infra

LAMBDA_PUSHES_TOPIC_ARN = arn:aws:sns:eu-west-1:760097843905:lambda_pushes

DOCKER_IMG_PUBLISH_LAMBDA    = wellcome/publish_lambda:12
DOCKER_IMG_BUILD_TEST_PYTHON = wellcome/build_test_python


# Publish a ZIP file containing a Lambda definition to S3.
#
# Args:
#   $1 - Path to the Lambda src directory, relative to the root of the repo.
#
define publish_lambda
	$(ROOT)/docker_run.py --aws --root -- \
		$(DOCKER_IMG_PUBLISH_LAMBDA) \
		"$(1)" --key="lambdas/$(1).zip" --bucket="$(WELLCOME_INFRA_BUCKET)" --sns-topic="$(LAMBDA_PUSHES_TOPIC_ARN)"
endef


# Test a Python project.
#
# Args:
#   $1 - Path to the Python project's directory, relative to the root
#        of the repo.
#
define test_python
	$(ROOT)/docker_run.py --aws --dind -- \
		$(DOCKER_IMG_BUILD_TEST_PYTHON) $(1)

	$(ROOT)/docker_run.py --aws --dind -- \
		--net=host \
		--volume $(ROOT)/shared_conftest.py:/conftest.py \
		--workdir $(ROOT)/$(1) --tty \
		wellcome/test_python_$(shell basename $(1)):latest
endef


# Define a series of Make tasks (test, publish) for a Python Lambda.
#
# Args:
#	$1 - Name of the target.
#	$2 - Path to the Lambda source directory.
#
define lambda_target_template
$(1)-test:
	$(call test_python,$(2))

$(1)-publish:
	$(call publish_lambda,$(2))

$(ROOT)/$(2)/requirements.txt: $(ROOT)/$(2)/requirements.in
	$(ROOT)/docker_run.py -- \
		--volume $(ROOT)/$(2)/src:/src micktwomey/pip-tools

$(ROOT)/$(2)/test_requirements.txt: $(ROOT)/$(2)/test_requirements.in
	$(ROOT)/docker_run.py -- \
		--volume $(ROOT)/$(2)/src:/src micktwomey/pip-tools \
		pip-compile test_requirements.in
endef
