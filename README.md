# xgboost-spark-sgx
This is a project for xgboost-spark running in sgx(occlum).

## Environment
* [XGBoost]()
* [Spark]()
* [SGX]

feature1,feature2,feature3,feature4,class

## Split And Encrypt
rm -rf ~/diankun/Output/* && \
java -cp target/xgboostsparksgx-1.0-SNAPSHOT-jar-with-dependencies.jar \
xgboostsparksgx.SplitAndEncrypt \
./data/iris.csv \
LDlxjm0y3HdGFniIGviJnMJbmFI+lt3dfIVyPJm1YSY= \
~/diankun/Output \
5

## Split And Encrypt For XGBoost
rm -rf ~/diankun/Output/* && \
java -cp target/xgboostsparksgx-1.0-SNAPSHOT-jar-with-dependencies.jar \
xgboostsparksgx.SplitAndEncryptForXgboost \
~/diankun/data/xgboost_data \
LDlxjm0y3HdGFniIGviJnMJbmFI+lt3dfIVyPJm1YSY= \
~/diankun/Output \
5

## Decrypt
$SPARK_HOME/bin/spark-submit   \
  --master local[4] \
  --class xgboostsparksgx.SparkDecryptFiles \
  --executor-cores 2 \
  --executor-memory 4G \
  target/xgboostsparksgx-1.0-SNAPSHOT-jar-with-dependencies.jar \
  ~/diankun/Output/iris \
  LDlxjm0y3HdGFniIGviJnMJbmFI+lt3dfIVyPJm1YSY=

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


#############
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
  /home/sdp/diankun/data/1G_data /home/sdp/diankun/process_data 4

$SPARK_HOME/bin/spark-submit   \
  --master local[8] \
  --conf spark.task.cpus=8 \
  --class xgboostsparksgx.xgbClassifierTrainingExample \
  --conf spark.scheduler.maxRegisteredResourcesWaitingTime=50000000 \
  --conf spark.worker.timeout=60000000 \
  --conf spark.network.timeout=10000000 \
  --conf spark.starvation.timeout=2500000 \
  --conf spark.speculation=false \
  --conf spark.executor.heartbeatInterval=10000000 \
  --conf spark.shuffle.io.maxRetries=5 \
  --conf spark.executor.instances=8 \
  --executor-cores 8 \
  --executor-memory 16G \
  --driver-memory 16G \
  target/xgboostsparksgx-1.0-SNAPSHOT-jar-with-dependencies.jar \
  /home/sdp/diankun/process_data_10g 8 1
