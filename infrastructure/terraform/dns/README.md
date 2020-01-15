# DNS terraform config

Managing the infrastructure for Wellcome Collection's DNS.

Most services should handle their own DNS by importing what they need
from here.

## Purpose

- To manage domains pointing to third-party services held outside of our
  infrastructure.
- Handling redirects.
- To expose state data from the Routemaster account so other Terraform
  provisionings can import that state. i.e. importing the Zone ID as
  from the output `weco_hosted_zone_id`.

## Issues
It's a known issue that the Routemaster account doesn't have the
`route53:GetChange` permission, so when creating records in the DNS zone
we get the error:

```sh
1 error occurred:
        * aws_route53_record.docs: 1 error occurred:
        * aws_route53_record.docs: AccessDenied: User: arn:aws:sts::250790015188:assumed-role/wellcomecollection-assume_role_hosted_zone_update/1579081975684193000 is not authorized to perform: route53:GetChange on resource: arn:aws:route53:::change/C8Q2UIJJMKTI1
        status code: 403, request id: c0498db6-7240-4072-a03c-466c7878fea1
```

This happens on the first apply, and won't happy again.

We are looking into how to resolve it.