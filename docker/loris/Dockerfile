FROM alpine

RUN apk add --update openssl
RUN wget https://github.com/loris-imageserver/loris/archive/v2.1.0-final.zip
RUN unzip v2.1.0-final.zip

WORKDIR loris-2.1.0-final
RUN apk add python py-setuptools

# User "loris" and or group "loris" do(es) not exist.
# Please create this user, e.g.:
#     `useradd -d /var/www/loris -s /sbin/false loris`
RUN mkdir -p /var/www/loris
RUN adduser -D -h /var/www/loris -s /bin/false loris

RUN python setup.py install
