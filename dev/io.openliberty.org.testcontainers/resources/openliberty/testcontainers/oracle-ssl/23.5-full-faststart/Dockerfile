# Base image used by external users is overwritten at runtime
# by a mirrored version from Artifactory for internal users.

ARG BASE_IMAGE="ghcr.io/gvenzl/oracle-free:23.5-full-faststart"

FROM ${BASE_IMAGE}

COPY setup/ /tmp/setup/
COPY startup/ /container-entrypoint-startdb.d

# Oracle admin moved from /opt/oracle/oradata/dbconfig/XE/ to /opt/oracle/homes/OraDBHome21cXE/network/admin in 21c
ENV ORACLE_ADMIN=/opt/oracle/homes/OraDBHome21cXE/network/admin

USER root

RUN chmod 777 /tmp/setup/enable_ssl.sh
RUN /tmp/setup/enable_ssl.sh
RUN rm -rf /tmp/setup

USER oracle

EXPOSE 1522
