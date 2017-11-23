FROM alpine

LABEL maintainer "Alex Chan <a.chan@wellcome.ac.uk>"
LABEL description "Push a new version of Swagger documentation to Spotlight"

RUN apk update
RUN apk add curl

COPY update_api_docs.sh /update_api_docs.sh

CMD ["/update_api_docs.sh"]
