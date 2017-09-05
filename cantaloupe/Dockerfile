FROM anapsix/alpine-java

RUN apk update
RUN apk add wget
RUN wget "https://github.com/medusa-project/cantaloupe/releases/download/v3.3.2/Cantaloupe-3.3.2.zip" -O Cantaloupe.zip
RUN unzip Cantaloupe.zip

WORKDIR /Cantaloupe-3.3.2

RUN apk add libjasper
RUN apk add jasper-libs --update-cache --repository http://dl-3.alpinelinux.org/alpine/edge/main/
RUN apk add graphicsmagick --update-cache --repository http://dl-3.alpinelinux.org/alpine/edge/community/
RUN which gm

RUN apk add libjpeg-turbo-dev freetype-dev zlib-dev

CMD ["/opt/jdk/bin/java", "-Dcantaloupe.config=/cantaloupe.properties", "-Xmx2g", "-jar", "Cantaloupe-3.3.2.war"]
