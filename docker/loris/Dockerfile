FROM ubuntu:14.04

# https://stackoverflow.com/a/23570150/1558022
ENV DEBIAN_FRONTEND noninteractive

RUN apt-get update
RUN apt-get install -y wget
RUN apt-get install -y unzip
RUN wget https://github.com/loris-imageserver/loris/archive/v2.1.0-final.zip
RUN unzip v2.1.0-final.zip

WORKDIR loris-2.1.0-final
RUN apt-get install -y python
RUN apt-get install -y python-setuptools

# User "loris" and or group "loris" do(es) not exist.
# Please create this user, e.g.:
#     `useradd -d /var/www/loris -s /sbin/false loris`
RUN useradd -d /var/www/loris -s /sbin/false loris

RUN apt-get install -y zlib1g-dev
RUN apt-get install -y libjpeg-turbo8-dev
RUN apt-get install -y python-dev
RUN apt-get install -y python-pip

RUN pip install configobj
RUN python setup.py install
