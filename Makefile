include functions.Makefile

include formatting.Makefile

include archive/Makefile

include infrastructure/critical/Makefile
include infrastructure/shared/Makefile

include assets/Makefile
include builds/Makefile
include loris/Makefile
include data_api/Makefile
include data_science/Makefile
include catalogue_api/Makefile
include catalogue_pipeline/Makefile
include goobi_adapter/Makefile
include monitoring/Makefile
include ontologies/Makefile
include reindexer/Makefile
include sbt_common/Makefile
include sierra_adapter/Makefile
include nginx/Makefile
include reporting/Makefile

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
