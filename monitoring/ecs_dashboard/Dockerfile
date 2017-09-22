FROM node:8

LABEL maintainer "Alex Chan <a.chan@wellcome.ac.uk>"
LABEL description "Used for deploying our ECS dashboard to AWS"

RUN apt-get update
RUN apt-get install --yes awscli jq

VOLUME ["/dashboard"]
WORKDIR /dashboard

CMD ["./deploy.sh"]
