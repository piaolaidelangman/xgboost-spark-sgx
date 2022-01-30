# xgboost-spark-sgx
This is a project for xgboost-spark running in sgx(occlum).

## Environment
* [XGBoost]()
* [Spark]()
* [SGX]

## Build Jar

rm -rf /home/sdp/diankun/process_data && \
$SPARK_HOME/bin/spark-submit   \
  --master local[4] \
  --conf spark.task.cpus=4  \
  --class xgboostsparksgx.PrepareData \
  --conf spark.executor.instances=8 \
  --executor-cores 8 \
  --total-executor-cores 64 \
  --executor-memory 8G \
  --conf spark.kryoserializer.buffer.max=1024m \
  target/xgboostsparksgx-1.0-SNAPSHOT-jar-with-dependencies.jar \
  /home/sdp/diankun/data/test_not_null_1g_data /home/sdp/diankun/process_data 1 

$SPARK_HOME/bin/spark-submit   \
  --master local[2] \
  --conf spark.task.cpus=1 \
  --class xgboostsparksgx.xgbClassifierTrainingExample \
  --conf spark.scheduler.maxRegisteredResourcesWaitingTime=5000000 \
  --conf spark.worker.timeout=60000 \
  --conf spark.network.timeout=10000000 \
  --conf spark.starvation.timeout=250000 \
  --conf spark.rpc.askTimeout=600 \
  --conf spark.io.compression.codec=lz4 \
  --conf spark.speculation=false \
  --conf spark.executor.heartbeatInterval=10000000 \
  --executor-cores 2 \
  target/xgboostsparksgx-1.0-SNAPSHOT-jar-with-dependencies.jar \
  /home/sdp/diankun/process_data 1 1



