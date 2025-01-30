# Base image used by external users is overwritten at runtime
# by a mirrored version from Artifactory for internal users.

ARG BASE_IMAGE="public.ecr.aws/docker/library/couchdb:3.2.0"

FROM ${BASE_IMAGE}

COPY couchdb-config/testcontainers_config.ini /opt/couchdb/etc/local.d/
COPY ssl-certs/couchdb.pem /etc/couchdb/cert/
COPY ssl-certs/privkey.pem /etc/couchdb/cert/

RUN chmod 644 /etc/couchdb/cert/*
