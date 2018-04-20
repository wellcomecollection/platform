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


sbt-common-test:
	$(call sbt_test,common)

sbt-common-publish:
	echo "Nothing to do!"


travistooling-test:
	# We deliberately *don't* run this in a Docker container, because the
	# scripts in this library are (currently) executed directly on the
	# Travis host.  We run the tests on the host as well, so the test
	# and actual environments are as close as possible.
	pip install --user -r $(ROOT)/travistooling/tests/test_requirements.txt
	coverage run --rcfile=travistooling/.coveragerc --module py.test travistooling/tests/test_*.py
	coverage report

travistooling-publish:
	$(error "Nothing to do for this task")
