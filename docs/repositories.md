# Overview

Code for the platform is split across multiple repositories.

You can see a list of all our repositories by [searching for the `wellcome-digital-platform` tag](https://github.com/search?type=Repositories&q=org%3Awellcometrust%20topic%3Awellcome-digital-platform).

This is an overview of how code for the various Wellcome Collection projects is split up:

![](weco-overview.png)

# Repositories

## Projects

*   [wellcometrust/platform](https://github.com/wellcometrust/platform): Shared infrastructure and docs.

    [![Build Status](https://travis-ci.org/wellcometrust/platform.svg?branch=master)](https://travis-ci.org/wellcometrust/platform)

*   [wellcometrust/catalogue](https://github.com/wellcometrust/catalogue): The Catalogue API & pipeline.

    [![Build Status](https://travis-ci.org/wellcometrust/catalogue.svg?branch=master)](https://travis-ci.org/wellcometrust/catalogue)

*   [wellcometrust/workflow](https://github.com/wellcometrust/catalogue): The Workflow service (infrastructure for Goobi).

    [![Build Status](https://travis-ci.org/wellcometrust/workflow.svg?branch=master)](https://travis-ci.org/wellcometrust/workflow)

*   [wellcometrust/storage-service](https://github.com/wellcometrust/storage-service): The archival storage service.

    [![Build Status](https://travis-ci.org/wellcometrust/storage-service.svg?branch=master)](https://travis-ci.org/wellcometrust/storage-service)

*   [wellcometrust/archivematica-infra](https://github.com/wellcometrust/archivematica-infra): Archivematica infrastructure for digital workflow.

    [![Build Status](https://travis-ci.org/wellcometrust/archivematica-infra.svg?branch=master)](https://travis-ci.org/wellcometrust/archivematica-infra)

## Build & Infrastructure

We use linux containers to encapsulate build dependencies and tools in order that wrangling with installing the correct set of dependencies or reprodcuing CI issues is minimised.

*   [wellcometrust/dockerfiles](https://github.com/wellcometrust/dockerfiles): Contains Dockerfiles for components of the platform, including most of our build system.

    [![Build Status](https://travis-ci.org/wellcometrust/dockerfiles.svg?branch=master)](https://travis-ci.org/wellcometrust/dockerfiles)

*   [wellcometrust/terraform-modules](https://github.com/wellcometrust/terraform-modules): Contains reusable Terraform modules that we use to define our infrastructure.

    [![Build Status](https://travis-ci.org/wellcometrust/terraform-modules.svg?branch=master)](https://travis-ci.org/wellcometrust/terraform-modules)

## Scala Libraries

The bulk of our code is written in Scala. There are a number of cross product libraries in use to standardise and speed up development.

*   [wellcometrust/wellcome-typesafe-app](https://github.com/wellcometrust/wellcome-typesafe-app): Base library for building Scala services.

    [![Build Status](https://travis-ci.org/wellcometrust/wellcome-typesafe-app.svg?branch=master)](https://travis-ci.org/wellcometrust/wellcome-typesafe-app)

*   [wellcometrust/scala-messaging](https://github.com/wellcometrust/scala-messaging): Provides helpers for interacting with SQS, SNS, and sending pointers via S3 if objects are too large.

    [![Build Status](https://travis-ci.org/wellcometrust/scala-messaging.svg?branch=master)](https://travis-ci.org/wellcometrust/scala-messaging)

*   [wellcometrust/scala-monitoring](https://github.com/wellcometrust/scala-monitoring): Provides wrappers for sending CloudWatch metrics.

    [![Build Status](https://travis-ci.org/wellcometrust/scala-monitoring.svg?branch=master)](https://travis-ci.org/wellcometrust/scala-monitoring)

*   [wellcometrust/scala-storage](https://github.com/wellcometrust/scala-storage): Provides wrappers around S3 and DynamoDB, including our "Versioned Hybrid Store" for storing large objects.

    [![Build Status](https://travis-ci.org/wellcometrust/scala-storage.svg?branch=master)](https://travis-ci.org/wellcometrust/scala-storage)

*   [wellcometrust/scala-json](https://github.com/wellcometrust/scala-json): Provides wrappers around interaction with `circe` (our chosen Scala lib).

    [![Build Status](https://travis-ci.org/wellcometrust/scala-json.svg?branch=master)](https://travis-ci.org/wellcometrust/scala-json)

*   [wellcometrust/scala-fixtures](https://github.com/wellcometrust/scala-fixtures): Provides wrappers around test fixtures shared by projects.

    [![Build Status](https://travis-ci.org/wellcometrust/scala-fixtures.svg?branch=master)](https://travis-ci.org/wellcometrust/scala-fixtures)

*   [wellcometrust/sierra-streams-source](https://github.com/wellcometrust/sierra-streams-source): Is a Scala library that provides Akka Streams from objects the [Sierra API](https://techdocs.iii.com/sierraapi/Content/titlePage.htm).

    Sierra is the library management system we use at Wellcome Collection, and one of the data sources for the platform.

    [![Build Status](https://travis-ci.org/wellcometrust/sierra-streams-source.svg?branch=master)](https://travis-ci.org/wellcometrust/sierra-streams-source)

## Python libraries

Most of our build tooling & some application code is written in Python, these libraries are shared across projects.

*   [AWS Utilities](https://github.com/wellcometrust/aws_utils) Is a Python library containing AWS-related utilities that we use in our AWS Lambdas