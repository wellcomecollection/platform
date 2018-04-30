include functions.Makefile

include formatting.Makefile

include assets/Makefile
include loris/Makefile
include shared_infra/Makefile
include data_api/Makefile
include catalogue_api/Makefile
include catalogue_pipeline/Makefile
include monitoring/Makefile
include ontologies/Makefile
include reindexer/Makefile
include sbt_common/Makefile
include sierra_adapter/Makefile
include nginx/Makefile


$(eval $(call sbt_library_template,common,common))


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
