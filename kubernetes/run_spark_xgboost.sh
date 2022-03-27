#!/bin/bash

${SPARK_HOME}/bin/spark-submit \
    --master k8s://https://${kubernetes_master_url}:6443 \
    --deploy-mode cluster \
    --name spark-xgboost-sgx \
    --class xgboostsparksgx.xgbClassifierTrainingExample \
    --conf spark.rpc.netty.dispatcher.numThreads=32 \
    --conf spark.kubernetes.container.image=$IMAGE \
    --conf spark.kubernetes.authenticate.driver.serviceAccountName=spark \
    --conf spark.kubernetes.executor.deleteOnTermination=false \
    --conf spark.kubernetes.driver.podTemplateFile=./executor.yaml \
    --conf spark.kubernetes.executor.podTemplateFile=./executor.yaml \
    --conf spark.kubernetes.file.upload.path=file:///tmp \
    --conf spark.driver.extraClassPath=local:///opt/spark/jars/* \
    --conf spark.executor.extraClassPath=local:///opt/spark/jars/* \
    --conf spark.task.cpus=2 \
    --num-executors 1 \
    --executor-cores 4 \
    --executor-memory 10g \
    --driver-memory 8g \
    local:/bin/jars/xgboostsparksgx-1.0-SNAPSHOT.jar \
    /host/data/xgboost 2 /host/data/model LDlxjm0y3HdGFniIGviJnMJbmFI+lt3dfIVyPJm1YSY= 2