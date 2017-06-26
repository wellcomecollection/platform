FROM alpine

RUN apk add --update openssl
RUN wget https://github.com/loris-imageserver/loris/archive/v2.1.0-final.zip && \
		unzip v2.1.0-final.zip && \
		rm v2.1.0-final.zip

WORKDIR loris-2.1.0-final
RUN apk add python py-setuptools

# User "loris" and or group "loris" do(es) not exist.
# Please create this user, e.g.:
#     `useradd -d /var/www/loris -s /sbin/false loris`
RUN mkdir -p /var/www/loris
RUN adduser -D -h /var/www/loris -s /bin/false loris

# Download error on https://pypi.python.org/simple/responses/: [SSL: CERTIFICATE_VERIFY_FAILED] certificate verify failed (_ssl.c:661) -- Some packages may not be found!
# Couldn't find index page for 'responses' (maybe misspelled?)
RUN apk add ca-certificates

# pillow deps
RUN apk add build-base jpeg-dev python-dev zlib-dev

RUN apk add py-pip
RUN pip install --upgrade pip

# idk why either
RUN pip install configobj

RUN python setup.py install
