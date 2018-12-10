# Platform

The Wellcome Collection digital platform, building APIs for searching and browsing our archive collections.

You can read documentation for our APIs at <https://developers.wellcomecollection.org/>.

You are free to copy, modify, and distribute Platform code with attribution under the terms of the MIT license. See the LICENSE file for details.

[![Build Status](https://travis-ci.org/wellcometrust/platform.svg?branch=master)](https://travis-ci.org/wellcometrust/platform)

## What's the "Wellcome digital platform"?

> Wellcome are developing a new digital platform for Wellcome Collection, that will enable us to improve the experience we offer to readers and researchers. We also want to make it easier for third-party developers to build things with our data and collections.

> This means providing programmatic access to our digital assets, metadata and web content by developing a simple, unified and coherent set of APIs.

### User led development
We intend to be led by the needs of the user first, and to be pragmatic about technical decisions in that light. Prototyping is part of the process of discovering the shape of an eventual API.

### Developing in the open
We intend to develop in the open, so that others can learn from our mistakes and successes. We also want to be transparent on the state of work in progress and provide a forum to raise issues and discuss approaches.

Everything we create will be open-source, under an MIT license. We will package things that are useful to others as standalone components, so that they can be easily re-used outside of our platform.

## Other repos

Code for the platform is split across multiple repositories.

You can see a list of all our repositories by [searching for the `wellcome-digital-platform` tag](https://github.com/search?type=Repositories&q=org%3Awellcometrust%20topic%3Awellcome-digital-platform).

These repositories include:

*   [wellcometrust/aws_utils][awsutils] is a Python library containing AWS-related utilities that we use in our AWS Lambdas

*   [wellcometrust/dockerfiles][dockerfiles] contains Dockerfiles for components of the platform, including most of our build system

*   [wellcometrust/terraform-modules][terraformmods] contains reusable Terraform modules that we use to define our infrastructure

The bulk of our code is written in Scala.
To reduce build times in the main repo, we've pushed out some of our libraries into external repositories:

*   [wellcometrust/scala-messaging][messaging] provides helpers for interacting with SQS, SNS, and sending pointers via S3 if objects are too large.

*   [wellcometrust/scala-monitoring][monitoring] provides wrappers for sending CloudWatch metrics.

*   [wellcometrust/scala-storage][storage] provides wrappers around S3 and DynamoDB, including our "Versioned Hybrid Store" for storing large objects.

*   [wellcometrust/sierra-streams-source][sierrastreams] is a Scala library that provides Akka Streams from objects in a [Sierra API][sierra].
    Sierra is the library management system we use at Wellcome Collection, and one of the data sources for the platform.

[awsutils]: https://github.com/wellcometrust/aws_utils
[dockerfiles]: https://github.com/wellcometrust/dockerfiles
[terraformmods]: https://github.com/wellcometrust/terraform-modules
[sierrastreams]: https://github.com/wellcometrust/sierra-streams-source
[messaging]: https://github.com/wellcometrust/scala-messaging
[monitoring]: https://github.com/wellcometrust/scala-monitoring
[storage]: https://github.com/wellcometrust/scala-storage
[sierra]: https://techdocs.iii.com/sierraapi/Default.htm
