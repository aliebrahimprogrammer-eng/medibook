FROM docker:cli AS docker-cli

FROM jenkins/jenkins:lts

USER root

COPY --from=docker-cli /usr/local/bin/docker /usr/local/bin/docker

RUN docker --version

USER jenkins
