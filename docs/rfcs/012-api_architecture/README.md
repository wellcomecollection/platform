# RFC 011: API Architecture

**Last updated: 09 January 2019.**

## Background

Wellcome Collection provides multiple APIs to interact with our data. A single API can be made up of multiple services, providing responses at different endpoints.

Some API endpoints require authentication, while others are public and would benefit from rate limiting.

## Problem statement

In order to provide a consistent location for Wellcome Collection APIs we'd like to serve all APIs from [api.wellcomecollection.org](https://api.wellcomecollection.org).

As of 09/01/2019 we are focusing on serving the `storage` and `catalogue` APIs from `api.wellcomecollection.org`. The `storage` API requires authentication, the catalogue API does not.

We are using [https://aws.amazon.com/api-gateway/](AWS API Gateway) to host our REST APIs and making use of a [https://docs.aws.amazon.com/apigateway/latest/developerguide/set-up-private-integration.html](private integration) to serve requests via an [https://aws.amazon.com/ecs/](AWS ECS Service).

We wish to segregate infrastructure projects into different AWS accounts for simplicity and security. See [../009-aws_account_layout/README.md0](RFC: 009-aws_account_layout).

Currently it is not possible to point a [https://docs.aws.amazon.com/apigateway/latest/developerguide/how-to-custom-domains.html](Custom Domain) from one account to another account using API Gateway.

## Proposed Solution

The recommended solution is to use an [https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/distribution-working-with.html](AWS CloudFront Distribution) to field requests to `api.wellcomecollection.org` and then use [https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/DownloadDistS3AndCustomOrigins.html#concept_CustomOrigin](Custom Origins) to serve requests from API Gateway in other accounts using their own Custom Domains.


See:

![](api.wellcomecollection.org.png)
