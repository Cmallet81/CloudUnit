# Base Dockerfile for all TOMCAT IMAGES
FROM cloudunit/base-12.04

# AJOUT DE LA VERSION DE TOMCAT
RUN wget https://github.com/Treeptik/cloudunit/releases/download/0.9/tomcat-appconf7.tar.gz -O /tmp/tomcat-appconf7.tar.gz \
	&& tar -xvf /tmp/tomcat-appconf7.tar.gz -C /cloudunit && rm /tmp/tomcat-appconf7.tar.gz

# ajout du dossier lib-ext

RUN mkdir -p /cloudunit/appconf/lib-ext
RUN mkdir -p /cloudunit/appconf/scripts

ADD scripts/ /cloudunit/appconf/scripts
RUN chmod +x /cloudunit/appconf/scripts/*/
/:q
;q::q:q


LABEL upgrade="commit"

# EXPOSE PORT
EXPOSE 22

VOLUME /cloudunit/appconf/logs

ENTRYPOINT ["/cloudunit/scripts/start-service.sh"]

