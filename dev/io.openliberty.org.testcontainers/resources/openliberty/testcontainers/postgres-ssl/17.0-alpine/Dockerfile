# Base image used by external users is overwritten at runtime
# by a mirrored version from Artifactory for internal users.

ARG BASE_IMAGE="public.ecr.aws/docker/library/postgres:17.0-alpine"

FROM ${BASE_IMAGE}

COPY ssl-certs/server.crt /var/lib/postgresql/server.crt
COPY ssl-certs/server.key /var/lib/postgresql/server.key

RUN chown postgres /var/lib/postgresql/server.key && chmod 600 /var/lib/postgresql/server.key && \
    chown postgres /var/lib/postgresql/server.crt && chmod 600 /var/lib/postgresql/server.crt

# Currently tagged in DockerHub as: kyleaure/postgres-ssl:1.0
