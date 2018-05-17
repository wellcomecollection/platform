include functions.Makefile

include formatting.Makefile

include assets/Makefile
include loris/Makefile
include shared_infra/Makefile
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


sbt-common-test:
	$(call sbt_test_no_docker,common)

sbt-common-publish:
	echo "Nothing to do!"


TRAVIS_LAMBDAS = snapshot_scheduler-test \
				reindex_job_creator-test \
				complete_reindex-test \
				reindex_shard_generator-test \
				sierra_window_generator-test \
				s3_demultiplexer-test \
				shared_infra-test \
				monitoring-test


travis-lambda-test:
	$(foreach task,$(TRAVIS_LAMBDAS),python run_travis_task.py $(task)-test;)

travis-lambda-publish:
	$(foreach task,$(TRAVIS_LAMBDAS),python run_travis_task.py $(task)-publish;)


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
