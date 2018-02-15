FROM openjdk:8-jre-slim


RUN apt-get update && apt-get install -y wget unzip gettext

RUN wget https://github.com/JanusGraph/janusgraph/releases/download/v0.2.0/janusgraph-0.2.0-hadoop2.zip

RUN unzip -q janusgraph-0.2.0-hadoop2.zip

RUN mkdir -p /janusgraph-0.2.0-hadoop2/ext/dynamodb-janusgraph-storage-backend

COPY ext/dynamodb-janusgraph-storage-backend /janusgraph-0.2.0-hadoop2/ext/.

COPY conf/gremlin-server/gremlin-server.yaml /janusgraph-0.2.0-hadoop2/conf/gremlin-server/.

RUN mkdir /template

COPY template/*.template /template/

COPY run_gremlin_server.sh /.

EXPOSE 8182

CMD ["/run_gremlin_server.sh"]
