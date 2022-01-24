FROM krallin/ubuntu-tini AS tini

FROM ubuntu:18.04 as spark

ARG SPARK_VERSION=3.1.2
ARG HADOOP_VERSION=3.2.0
ENV HADOOP_VERSION=${HADOOP_VERSION}
ENV SPARK_VERSION=${SPARK_VERSION}
ENV SPARK_HOME=/opt/spark

ARG SPARK_JAR_REPO_URL

ARG HTTP_PROXY_HOST
ARG HTTP_PROXY_PORT
ARG HTTPS_PROXY_HOST
ARG HTTPS_PROXY_PORT

RUN apt-get update && DEBIAN_FRONTEND="noninteractive" apt-get install -y --no-install-recommends \
        openjdk-8-jdk build-essential wget git && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# maven
RUN cd /opt && \
    wget https://archive.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz && \
    tar -zxvf apache-maven-3.6.3-bin.tar.gz

# spark
# TODO change to build from source instead of download from spark binary
RUN cd /opt && \
    wget https://archive.apache.org/dist/spark/spark-${SPARK_VERSION}/spark-${SPARK_VERSION}-bin-hadoop3.2.tgz && \
    tar -zxvf spark-${SPARK_VERSION}-bin-hadoop3.2.tgz && \
    mv spark-${SPARK_VERSION}-bin-hadoop3.2 spark && \
    rm spark-${SPARK_VERSION}-bin-hadoop3.2.tgz && \
    cp spark/conf/log4j.properties.template spark/conf/log4j.properties && \
    echo $'\nlog4j.logger.io.netty=ERROR' >> spark/conf/log4j.properties && \
    rm spark/python/lib/pyspark.zip && \
    rm spark/jars/spark-core_2.12-$SPARK_VERSION.jar && \
    rm spark/jars/spark-kubernetes_2.12-$SPARK_VERSION.jar && \
    rm spark/jars/spark-network-common_2.12-$SPARK_VERSION.jar && \
    rm spark/jars/hadoop-common-3.2.0.jar && \
    rm spark/jars/hive-exec-2.3.7-core.jar

# spark modification
RUN cd /opt && \
    wget $SPARK_JAR_REPO_URL/spark-core_2.12-$SPARK_VERSION.jar && \
    wget $SPARK_JAR_REPO_URL/spark-kubernetes_2.12-$SPARK_VERSION.jar && \
    wget $SPARK_JAR_REPO_URL/spark-network-common_2.12-$SPARK_VERSION.jar && \
    wget $SPARK_JAR_REPO_URL/pyspark.zip && \
    mv /opt/spark-core_2.12-$SPARK_VERSION.jar  /opt/spark/jars/spark-core_2.12-$SPARK_VERSION.jar && \
    mv /opt/spark-kubernetes_2.12-$SPARK_VERSION.jar /opt/spark/jars/spark-kubernetes_2.12-$SPARK_VERSION.jar && \
    mv /opt/spark-network-common_2.12-$SPARK_VERSION.jar /opt/spark/jars/spark-network-common_2.12-$SPARK_VERSION.jar && \
    mv /opt/pyspark.zip /opt/spark/python/lib/pyspark.zip

RUN mkdir -p /opt/src

# hadoop
RUN cd /opt/src && \
    wget https://github.com/protocolbuffers/protobuf/releases/download/v2.5.0/protobuf-2.5.0.tar.bz2 && \
    tar jxvf protobuf-2.5.0.tar.bz2 && \
    cd protobuf-2.5.0 && \
    ./configure && \
    make && \
    make check && \
    export LD_LIBRARY_PATH=/usr/local/lib && \
    make install && \
    rm -f protobuf-2.5.0.tar.bz2 && \
    cd /opt/src && \
    git clone https://github.com/analytics-zoo/hadoop.git && \
    cd hadoop && \
    git checkout branch-3.2.0-ppml && \
    cd hadoop-common-project/hadoop-common && \
    export MAVEN_OPTS="-Xmx2g -XX:ReservedCodeCacheSize=512m \
        -Dhttp.proxyHost=$HTTP_PROXY_HOST \
        -Dhttp.proxyPort=$HTTP_PROXY_PORT \
        -Dhttps.proxyHost=$HTTPS_PROXY_HOST \
        -Dhttps.proxyPort=$HTTPS_PROXY_PORT" && \
    /opt/apache-maven-3.6.3/bin/mvn -T 16 -DskipTests=true clean package


FROM occlum/occlum:0.26.0-ubuntu18.04 as ppml

ARG SPARK_VERSION=3.1.2
ARG HADOOP_VERSION=3.2.0
ENV HADOOP_VERSION=${HADOOP_VERSION}
ENV SPARK_VERSION=${SPARK_VERSION}
ENV SPARK_HOME=/opt/spark
ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
ENV SGX_MEM_SIZE=20GB
ENV TPCH_DIR=/opt/zoo-tutorials/tpch-spark

ARG HTTP_PROXY_HOST
ARG HTTP_PROXY_PORT
ARG HTTPS_PROXY_HOST
ARG HTTPS_PROXY_PORT

RUN echo "auth required pam_wheel.so use_uid" >> /etc/pam.d/su && \
    chgrp root /etc/passwd && chmod ug+rw /etc/passwd 

COPY --from=tini /usr/local/bin/tini /sbin/tini



RUN apt-get update && \
    DEBIAN_FRONTEND="noninteractive" apt-get install -y --no-install-recommends \
        openjdk-11-jdk && \
    apt-get clean

# prepare Spark
COPY --from=spark /opt/spark /opt/spark
COPY --from=spark /opt/libhadoop.so /opt/libhadoop.so

# Prepare xgboost-spark-sgx
RUN git clone https://github.com/piaolaidelangman/xgboost-spark-sgx.git && \
    mvn clean package


# Copy scripts & other files
ADD ./run_spark_on_occlum_glibc.sh /opt/run_spark_on_occlum_glibc.sh


COPY ./entrypoint.sh /opt/

RUN chmod a+x /opt/entrypoint.sh && \
    chmod a+x /opt/run_spark_on_occlum_glibc.sh

ENTRYPOINT [ "/opt/entrypoint.sh" ]
