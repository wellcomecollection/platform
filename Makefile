include functions.Makefile
include formatting.Makefile

include infrastructure/critical/Makefile
include infrastructure/shared/Makefile

include assets/Makefile
include builds/Makefile
include loris/Makefile
include data_science/Makefile
include monitoring/Makefile
include ontologies/Makefile
include sbt_common/Makefile
include nginx/Makefile
include reporting/Makefile
include storage/Makefile

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
