# Image Authentication

## Problem Statement

Some image assets served via the IIIF Image API compliant server [Loris](https://github.com/loris-imageserver/loris), are restricted and require authentication before viewing. The IIIF Image standard requires that image asset URLs follow the [described syntax](http://iiif.io/api/image/2.1/#canonical-uri-syntax).

We need to restrict access for certain images based on their rights status and the authentication status / role of the viewer.

In addition we need to be able to scalably serve these images via a CDN (in our case CloudFront). This means not requiring sign-in for all users to prevent the cache varying on authentication tokens.

## Suggested Solution

We propose to build an authentication solution based on introducing an origin-response [lambda@edge](https://docs.aws.amazon.com/lambda/latest/dg/lambda-edge.html) function.

![iiif image authentication proposal - page 1 1](https://user-images.githubusercontent.com/953792/39766686-c5df6b6c-52dc-11e8-9434-4635d5855f21.png)

### Process flow

The authentication flow is as follows:

![image auth flow - page 1 1](https://user-images.githubusercontent.com/953792/39766720-d217ff84-52dc-11e8-908f-4f660ac1402c.png)
