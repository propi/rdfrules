# docker build -t rdfrules .
# docker run --name rdfrules -p 8899:8899 -d rdfrules

FROM debian:8.10

LABEL maintainer="prozeman@gmail.com"

WORKDIR /root

RUN echo 'deb http://http.debian.net/debian jessie-backports main' >> /etc/apt/sources.list && \
    echo 'deb http://dl.bintray.com/sbt/debian /' >> /etc/apt/sources.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823 && \
    apt-get update && \
    apt-get install -t jessie-backports -y --no-install-recommends openjdk-8-jdk
       
ADD . /root

RUN chmod 775 bin/main && \
    sed -i 's/"http:\/\/localhost:8080\/api"/window.location.href.replace(\/\\\/webapp\\\/.*\/, "")/g' webapp/index.html

ENV RDFRULES_HOSTNAME="0.0.0.0"
ENV RDFRULES_PORT="8899"
ENV RDFRULES_STOPPING_TOKEN="stop-1144"
ENV RDFRULES_WORKSPACE="/root/webapp/workspace"
ENV RDFRULES_WEBAPP_DIR="/root/webapp"
ENV JAVA_OPTS="-Duser.country=US -Dorg.slf4j.simpleLogger.defaultLogLevel=info -Duser.language=en -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true"

EXPOSE 8899

CMD ["bash", "/root/bin/main"]