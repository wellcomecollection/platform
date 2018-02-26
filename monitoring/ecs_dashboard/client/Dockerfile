FROM alpine

LABEL maintainer "Digital Platform <wellcomedigitalplatform@wellcome.ac.uk>"
LABEL description "Used for deploying our ECS dashboard to AWS"

RUN apk update && apk add jq nodejs py2-pip
RUN pip install awscli

VOLUME ["/dashboard"]
WORKDIR /dashboard

CMD ["./deploy.sh"]
