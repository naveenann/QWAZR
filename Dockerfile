FROM java:openjdk-8-jdk-alpine

RUN mkdir /usr/share/qwazr
RUN wget -O /usr/share/qwazr/qwazr.jar http://download.qwazr.com/latest/qwazr-server-1.1-SNAPSHOT-exec.jar

VOLUME /var/lib/qwazr

EXPOSE 9090 9091

WORKDIR /var/lib/qwazr/

CMD ["java", "-Dfile.encoding=UTF-8", "-jar", "/usr/share/qwazr/qwazr.jar"]