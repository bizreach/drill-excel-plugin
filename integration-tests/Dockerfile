# https://github.com/apache/drill/blob/master/distribution/Dockerfile
FROM centos:7

ARG java_version
ARG drill_version

RUN yum install -y java-${java_version}-openjdk-devel which ; yum clean all ; rm -rf /var/cache/yum \
    && curl -O https://archive.apache.org/dist/drill/drill-${drill_version}/apache-drill-${drill_version}.tar.gz \
    && mkdir /opt/drill \
    && tar -xvzf /apache-drill-${drill_version}.tar.gz --directory=/opt/drill --strip-components 1

COPY target/*-jar-with-dependencies.jar /opt/drill/jars/3rdparty/
COPY integration-tests/storage-plugins-override.conf /opt/drill/conf/
COPY integration-tests/emp.xlsx /tmp/

ENTRYPOINT /opt/drill/bin/drill-embedded
