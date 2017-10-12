ROOT = $(shell git rev-parse --show-toplevel)
BUILDS = $(ROOT)/builds


PY36_CI = $(BUILDS)/python3.6_ci

# TODO: Flip this to using micktwomey/pip-tools when that's updated
# with a newer version of pip-tools.
$(PY36_CI)/requirements.txt: $(PY36_CI)/requirements.in
	docker run \
		--volume $(PY36_CI):/data \
		--env OP=build-lock-file \
		python3.6_ci:latest
	touch $(PY36_CI)/requirements.txt

# Note that requirements.txt is *not* a target of this task, to avoid
# Travis just building a new version of the file on every run.
py36_targets := $(PY36_CI)/Dockerfile $(wildcard $(PY36_CI)/src/*)

$(ROOT)/.docker/python3.6_ci: $(py36_targets)
	$(ROOT)/builds/build_ci_docker_image.py --project=python3.6_ci --dir=$(PY36_CI)
