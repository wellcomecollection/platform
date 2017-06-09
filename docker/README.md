# Dockerfiles

Some of our supporting services (nginx/Jenkins) are built and configured using Dockerfiles.

The container images are pushed to ECR and pulled in to ECS Tasks.

## Updating images

In order to update the images used by services, we currently need to build and push images locally.

For example for `nginx`:

```sh
cd folder_with_dockerfile/
docker build --rm -t nginx_servicename .
docker tag $(docker images -q nginx_servicename) ecr.repo.uri.com/uk.ac.wellcome/nginx:servicename
$(aws ecr get-login)
docker push ecr.repo.uri.com/uk.ac.wellcome/nginx:servicename
```
