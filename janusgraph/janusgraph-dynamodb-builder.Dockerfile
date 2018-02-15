FROM openjdk:8-jdk

RUN apt-get update && apt-get install -y maven git docker

RUN git clone https://github.com/amcp/dynamodb-janusgraph-storage-backend.git

WORKDIR /dynamodb-janusgraph-storage-backend

RUN git checkout 020

RUN mvn install

RUN mkdir -p /build/ext/dynamodb-janusgraph-storage-backend
RUN mkdir -p /build/conf/gremlin-server
RUN mkdir -p /build/template

RUN cp /dynamodb-janusgraph-storage-backend/target/dynamodb-janusgraph-storage-backend-1.2.0.jar /build/ext/dynamodb-janusgraph-storage-backend/.
RUN cp -R /dynamodb-janusgraph-storage-backend/target/dependencies /build/ext/dynamodb-janusgraph-storage-backend/.

VOLUME /data

RUN mkdir /template

COPY build_janusgraph_dynamodb_image.sh /.

ENV tag "no-endpoint"

ENTRYPOINT /build_janusgraph_dynamodb_image.sh $tag