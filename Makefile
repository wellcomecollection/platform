ROOT = $(shell git rev-parse --show-toplevel)
INFRA_BUCKET = wellcomecollection-platform-infra

WELLCOME_INFRA_BUCKET      = wellcomecollection-platform-infra
WELLCOME_MONITORING_BUCKET = wellcomecollection-platform-monitoring

include functions.Makefile

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
