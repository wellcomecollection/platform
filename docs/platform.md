# Wellcome Digital Platform service guidelines

How to build a new service for the Wellcome Digital Platform

## Overview

Services for the Wellcome Digital Platform are deployed on AWS and are likely to be a mix of ECS tasks and lambdas. The languages we use are Scala & Python.

There are 3 kinds of project currently in use:
- ECS "script" Tasks: One off or scheduled tasks running via the ECS RunTask API.
- ECS Services: Long running services e.g. APIs
- Lambdas: Very short running or scheduled tasks taking advantage of AWS event streams where appropriate.

## Project structure

- Infrastructure description (`./infra`)
  - Using terraform (separated by stack where appropriate).
    It's expected that some services will share terraform state where there is a high degree of coupling.
  - Making use of shared terraform uk.ac.wellcome.finatra.modules from project root (`ecs_service` etc)

- `Makefile`: Describing project specific tasks. Where steps fail they should exit with a non-zero exit code status.
  The following steps are expected:
  - `build`: build deployment artefacts locally
  - `test`: run project tests
    There are container images provided for running simple linting/testing steps.
  - `run`: run project locally
  - `publish`: publish project artefacts to remote store before deployment:
     This is likely to push your container description(s) to ECR/docker hub
  - Terraform tasks using provided `terraform_ci` container:
    - `tf-plan`
    - `tf-apply`

- `README.md`
  - With the following suggested sections:
    - Purpose of project
    - High level technical overview of services (how it interacts with other services, dependencies)

## Continuous Integration

Currently our project builds in [Travis CI](https://travis-ci.org/wellcometrust/platform).

Add your `Make` tasks to the build matrix in `/.travis.yml`.

```yml
env:
  global:
    - ...
  matrix:
    - TASK=my_project-test
    - TASK=...
```

## Monitoring

### CloudWatch Metrics

We use Grafana at: [https://monitoring.wellcomecollection.org](https://monitoring.wellcomecollection.org)

This is available only to Wellcome internal IP addresses.

You can build your own dashboards based on CloudWatch data that will be persisted when saved.

### ECS Service dashboard

ECS Cluster and Service status can be reviewed using our [ECS Dashboard](https://s3-eu-west-1.amazonaws.com/wellcome-platform-dash/index.html).

The dashboard will update automatically to include new services and clusters within the `digital-platform` AWS Account.

### Slack Bot

CloudWatch Alarms can be directed to the `post_to_slack` lambda via an SNS topic.

Alerts will appear in the `#digital-platform` Wellcome Slack channel.

