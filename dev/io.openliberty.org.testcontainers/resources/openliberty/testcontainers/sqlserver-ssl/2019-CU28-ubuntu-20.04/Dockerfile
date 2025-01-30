# Base image used by external users is overwritten at runtime
# by a mirrored version from Artifactory for internal users.

ARG BASE_IMAGE="mcr.microsoft.com/mssql/server:2019-CU28-ubuntu-20.04"

FROM ${BASE_IMAGE}

COPY --chown=mssql:0 container-files/mssql.conf      /var/opt/mssql/mssql.conf
COPY --chown=mssql:0 container-files/mssql.pem       /etc/ssl/certs/mssql.pem
COPY --chown=mssql:0 container-files/mssql.key       /etc/ssl/mssql.key
COPY --chown=mssql:0 container-files/truststore.p12  /truststore.p12

