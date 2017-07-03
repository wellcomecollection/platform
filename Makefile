docker_jslint:
	docker build ./docker/jslint_ci --tag jslint_ci

docker_flake8:
	docker build ./docker/python3.6_ci --tag python3.6_ci

lint-ontologies: docker_jslint
	docker run -v $$(pwd)/ontologies:/data jslint_ci:latest

lint-lambdas: docker_flake8
	docker run -v $$(pwd)/lambdas:/data python3.6_ci:latest
