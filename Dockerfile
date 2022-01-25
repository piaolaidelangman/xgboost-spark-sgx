FROM intelanalytics/bigdl-ppml-trusted-big-data-ml-scala-occlum:0.14.0-SNAPSHOT

# Prepare xgboost-spark-sgx
# RUN git clone https://github.com/piaolaidelangman/xgboost-spark-sgx.git && \
#     mvn clean package

RUN rm /opt/run_spark_on_occlum_glibc.sh

# Copy scripts & other files
ADD xgboostsparksgx-1.0-SNAPSHOT-jar-with-dependencies.jar /opt
ADD ./run_spark_on_occlum_glibc.sh /opt/run_spark_on_occlum_glibc.sh

RUN chmod a+x /opt/run_spark_on_occlum_glibc.sh
