FROM intelanalytics/bigdl-ppml-trusted-big-data-ml-scala-occlum:0.14.0-SNAPSHOT

COPY --from=tini /opt/apache-maven-3.6.3 /opt/apache-maven-3.6.3

# Prepare xgboost-spark-sgx
RUN cd /opt && \
    git clone https://github.com/piaolaidelangman/xgboost-spark-sgx.git && \
    cd ./xgboost-spark-sgx && /opt/apache-maven-3.6.3/bin/mvn clean package

# Copy scripts & other files
ADD ./run_spark_on_occlum_glibc.sh /opt/run_spark_on_occlum_glibc.sh

RUN chmod a+x /opt/run_spark_on_occlum_glibc.sh
