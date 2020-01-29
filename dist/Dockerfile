# docker build -t rdfrules .
# docker run --name rdfrules -p 8899:8899 -d rdfrules

FROM debian:9.11

LABEL maintainer="prozeman@gmail.com"

WORKDIR /root

RUN apt-get update && \
    apt-get install -y --no-install-recommends openjdk-8-jdk
       
ADD . /root

RUN chmod 775 bin/main && \
    sed -i 's/"http:\/\/localhost:8851\/api"/window.location.href.replace(\/\\\/webapp\\\/.*\/, "")/g' webapp/index.html

ENV RDFRULES_HOSTNAME="0.0.0.0"
ENV RDFRULES_PORT="8899"
ENV RDFRULES_STOPPING_TOKEN="stop-1144"
ENV RDFRULES_WORKSPACE="/root/webapp/workspace"
ENV RDFRULES_WEBAPP_DIR="/root/webapp"
ENV JAVA_OPTS="-Duser.country=US -Dorg.slf4j.simpleLogger.defaultLogLevel=info -Duser.language=en -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true"

EXPOSE 8899

CMD ["bash", "/root/bin/main"]