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