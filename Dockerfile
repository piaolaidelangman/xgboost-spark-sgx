# ARG SPARK_VERSION=3.1.2
# ARG HADOOP_VERSION=3.2.0

# FROM krallin/ubuntu-tini AS tini

# FROM ubuntu:20.04 as spark

# ARG SPARK_VERSION
# ARG HADOOP_VERSION
# ARG SPARK_SCALA_VERSION=2.12
# ENV HADOOP_VERSION=${HADOOP_VERSION}
# ENV SPARK_VERSION=${SPARK_VERSION}
# ENV SPARK_SCALA_VERSION=${SPARK_SCALA_VERSION}
# ENV SPARK_HOME=/opt/spark

# ARG SPARK_JAR_REPO_URL

# ARG HTTP_PROXY_HOST
# ARG HTTP_PROXY_PORT
# ARG HTTPS_PROXY_HOST
# ARG HTTPS_PROXY_PORT
# ENV HTTP_PROXY=http://$HTTP_PROXY_HOST:$HTTP_PROXY_PORT
# ENV HTTPS_PROXY=http://$HTTPS_PROXY_HOST:$HTTPS_PROXY_PORT

# RUN apt-get update && DEBIAN_FRONTEND="noninteractive" apt-get install -y --no-install-recommends \
#         openjdk-8-jdk build-essential wget git unzip && \
#     apt-get clean && \
#     rm -rf /var/lib/apt/lists/*

# # maven
# RUN cd /opt && \
#     wget https://archive.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz && \
#     tar -zxvf apache-maven-3.6.3-bin.tar.gz

# # spark
# RUN cd /opt && \
#     wget https://archive.apache.org/dist/spark/spark-${SPARK_VERSION}/spark-${SPARK_VERSION}-bin-hadoop3.2.tgz && \
#     tar -zxvf spark-${SPARK_VERSION}-bin-hadoop3.2.tgz && \
#     mv spark-${SPARK_VERSION}-bin-hadoop3.2 spark && \
#     rm spark-${SPARK_VERSION}-bin-hadoop3.2.tgz && \
#     cp spark/conf/log4j.properties.template spark/conf/log4j.properties && \
#     echo $'\nlog4j.logger.io.netty=ERROR' >> spark/conf/log4j.properties && \
#     rm spark/python/lib/pyspark.zip && \
#     rm spark/jars/spark-core_2.12-$SPARK_VERSION.jar && \
#     rm spark/jars/spark-kubernetes_2.12-$SPARK_VERSION.jar && \
#     rm spark/jars/spark-network-common_2.12-$SPARK_VERSION.jar && \
#     rm spark/jars/hadoop-common-3.2.0.jar && \
#     rm spark/jars/hive-exec-2.3.7-core.jar && \
#     rm spark/jars/slf4j-log4j12-1.7.30.jar && \
#     rm spark/jars/log4j-1.2.17.jar && \
#     wget -P /opt/spark/jars/ https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-1.2-api/2.17.1/log4j-1.2-api-2.17.1.jar && \
#     wget -P /opt/spark/jars/ https://repo1.maven.org/maven2/org/slf4j/slf4j-reload4j/1.7.35/slf4j-reload4j-1.7.35.jar && \
#     wget -P /opt/spark/jars/ https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-api/2.17.1/log4j-api-2.17.1.jar && \
#     wget -P /opt/spark/jars/ https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-core/2.17.1/log4j-core-2.17.1.jar && \
#     wget -P /opt/spark/jars/ https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-slf4j-impl/2.17.1/log4j-slf4j-impl-2.17.1.jar

# # spark modification
# RUN cd /opt && \
#     wget $SPARK_JAR_REPO_URL/spark-core_2.12-$SPARK_VERSION.jar && \
#     wget $SPARK_JAR_REPO_URL/spark-kubernetes_2.12-$SPARK_VERSION.jar && \
#     wget $SPARK_JAR_REPO_URL/spark-network-common_2.12-$SPARK_VERSION.jar && \
#     wget $SPARK_JAR_REPO_URL/pyspark.zip && \
#     mv /opt/spark-core_2.12-$SPARK_VERSION.jar  /opt/spark/jars/spark-core_2.12-$SPARK_VERSION.jar && \
#     mv /opt/spark-kubernetes_2.12-$SPARK_VERSION.jar /opt/spark/jars/spark-kubernetes_2.12-$SPARK_VERSION.jar && \
#     mv /opt/spark-network-common_2.12-$SPARK_VERSION.jar /opt/spark/jars/spark-network-common_2.12-$SPARK_VERSION.jar && \
#     mv /opt/pyspark.zip /opt/spark/python/lib/pyspark.zip

# RUN mkdir -p /opt/src

# # hadoop
# RUN cd /opt/src && \
#     wget https://github.com/protocolbuffers/protobuf/releases/download/v2.5.0/protobuf-2.5.0.tar.bz2 && \
#     tar jxvf protobuf-2.5.0.tar.bz2 && \
#     cd protobuf-2.5.0 && \
#     ./configure && \
#     make && \
#     make check && \
#     export LD_LIBRARY_PATH=/usr/local/lib && \
#     make install && \
#     rm -f protobuf-2.5.0.tar.bz2 && \
#     cd /opt/src && \
#     git clone https://github.com/analytics-zoo/hadoop.git && \
#     cd hadoop && \
#     git checkout branch-3.2.0-ppml && \
#     cd hadoop-common-project/hadoop-common && \
#     export MAVEN_OPTS="-Xmx2g -XX:ReservedCodeCacheSize=512m \
#         -Dhttp.proxyHost=$HTTP_PROXY_HOST \
#         -Dhttp.proxyPort=$HTTP_PROXY_PORT \
#         -Dhttps.proxyHost=$HTTPS_PROXY_HOST \
#         -Dhttps.proxyPort=$HTTPS_PROXY_PORT" && \
#     /opt/apache-maven-3.6.3/bin/mvn -T 16 -DskipTests=true clean package

# # Remove fork with libhadoop.so and spark-network-common.jar
# RUN wget https://sourceforge.net/projects/analytics-zoo/files/analytics-zoo-data/libhadoop.so -P /opt/ && \
#     cp -f /opt/src/hadoop/hadoop-common-project/hadoop-common/target/hadoop-common-${HADOOP_VERSION}.jar ${SPARK_HOME}/jars && \
#     rm -rf /opt/src

# FROM occlum/occlum:0.27.0-ubuntu20.04 as ppml

# ARG SPARK_VERSION
# ARG HADOOP_VERSION
# ENV HADOOP_VERSION=${HADOOP_VERSION}
# ENV SPARK_VERSION=${SPARK_VERSION}
# ENV SPARK_HOME=/opt/spark

# ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
# ENV SGX_MEM_SIZE=20GB

# ARG HTTP_PROXY_HOST
# ARG HTTP_PROXY_PORT
# ARG HTTPS_PROXY_HOST
# ARG HTTPS_PROXY_PORT

# RUN echo "auth required pam_wheel.so use_uid" >> /etc/pam.d/su && \
#     chgrp root /etc/passwd && chmod ug+rw /etc/passwd

# COPY --from=tini /usr/local/bin/tini /sbin/tini

# RUN apt-get update && \
#     DEBIAN_FRONTEND="noninteractive" apt-get install -y --no-install-recommends \
#         openjdk-11-jdk && \
#     apt-get clean

# # prepare Spark
# COPY --from=spark /opt/spark /opt/spark
# COPY --from=spark /opt/libhadoop.so /opt/libhadoop.so

# # Copy scripts & other files
# ADD ./run_spark_on_occlum_glibc.sh /opt/run_spark_on_occlum_glibc.sh
# ADD ./log4j2.xml /opt/spark/conf/log4j2.xml
# ADD target/xgboostsparksgx-1.0-SNAPSHOT-jar-with-dependencies.jar /opt

# COPY ./entrypoint.sh /opt/

# RUN chmod a+x /opt/entrypoint.sh && \
#     chmod a+x /opt/run_spark_on_occlum_glibc.sh

# ENTRYPOINT [ "/opt/entrypoint.sh" ]

FROM intelanalytics/bigdl-ppml-trusted-big-data-ml-scala-occlum:2.1.0-SNAPSHOT
# Copy scripts & other files
#RUN rm /opt/run_spark_on_occlum_glibc.sh
#ADD ./run_spark_on_occlum_glibc.sh /opt/run_spark_on_occlum_glibc.sh
#ADD target/xgboostsparksgx-1.0-SNAPSHOT-jar-with-dependencies.jar /opt
ADD target/xgboostsparksgx-1.0-SNAPSHOT.jar /opt

RUN chmod a+x /opt/entrypoint.sh && \
    chmod a+x /opt/run_spark_on_occlum_glibc.sh

ENTRYPOINT [ "/opt/entrypoint.sh" ]
