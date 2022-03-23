#!/bin/bash
set -x

BLUE='\033[1;34m'
NC='\033[0m'
occlum_glibc=/opt/occlum/glibc/lib
# occlum-node IP
HOST_IP=`cat /etc/hosts | grep $HOSTNAME | awk '{print $1}'`

check_sgx_dev() {
    if [ -c "/dev/sgx/enclave" ]; then
        echo "/dev/sgx/enclave is ready"
    elif [ -c "/dev/sgx_enclave" ]; then
        echo "/dev/sgx/enclave not ready, try to link to /dev/sgx_enclave"
        mkdir -p /dev/sgx
        ln -s /dev/sgx_enclave /dev/sgx/enclave
    else
        echo "both /dev/sgx/enclave /dev/sgx_enclave are not ready, please check the kernel and driver"
    fi

    if [ -c "/dev/sgx/provision" ]; then
        echo "/dev/sgx/provision is ready"
    elif [ -c "/dev/sgx_provision" ]; then
        echo "/dev/sgx/provision not ready, try to link to /dev/sgx_provision"
        mkdir -p /dev/sgx
        ln -s /dev/sgx_provision /dev/sgx/provision
    else
        echo "both /dev/sgx/provision /dev/sgx_provision are not ready, please check the kernel and driver"
    fi

    ls -al /dev/sgx
}

init_instance() {
    # check and fix sgx device
    check_sgx_dev
    # Init Occlum instance
    cd /opt
    # check if occlum_spark exists
    [[ -d occlum_spark ]] || mkdir occlum_spark
    cd occlum_spark
    /opt/occlum/start_aesm.sh
    occlum init
    new_json="$(jq '.resource_limits.user_space_size = "SGX_MEM_SIZE" |
        .resource_limits.max_num_of_threads = "SGX_THREAD" |
        .process.default_heap_size = "SGX_HEAP" |
        .resource_limits.kernel_space_heap_size="SGX_KERNEL_HEAP" |
        .entry_points = [ "/usr/lib/jvm/java-11-openjdk-amd64/bin" ] |
        .env.untrusted = [ "DMLC_TRACKER_URI", "SPARK_DRIVER_URL", "SPARK_TESTING" ] |
        .env.default = [ "LD_LIBRARY_PATH=/usr/lib/jvm/java-11-openjdk-amd64/lib/server:/usr/lib/jvm/java-11-openjdk-amd64/lib:/usr/lib/jvm/java-11-openjdk-amd64/../lib:/lib","SPARK_CONF_DIR=/bin/conf","SPARK_ENV_LOADED=1","PYTHONHASHSEED=0","SPARK_HOME=/bin","SPARK_SCALA_VERSION=2.12","SPARK_JARS_DIR=/bin/jars","LAUNCH_CLASSPATH=/bin/jars/*",""]' Occlum.json)" && \
    echo "${new_json}" > Occlum.json
    echo "SGX_MEM_SIZE ${SGX_MEM_SIZE}"

    if [[ -z "$META_SPACE" ]]; then
        echo "META_SPACE not set, using default value 256m"
        META_SPACE=256m
    else
        echo "META_SPACE=$META_SPACE"
    fi

    if [[ -z "$SGX_MEM_SIZE" ]]; then
        sed -i "s/SGX_MEM_SIZE/20GB/g" Occlum.json
    else
        sed -i "s/SGX_MEM_SIZE/${SGX_MEM_SIZE}/g" Occlum.json
    fi

    if [[ -z "$SGX_THREAD" ]]; then
        sed -i "s/\"SGX_THREAD\"/512/g" Occlum.json
    else
        sed -i "s/\"SGX_THREAD\"/${SGX_THREAD}/g" Occlum.json
    fi

    if [[ -z "$SGX_HEAP" ]]; then
        sed -i "s/SGX_HEAP/512MB/g" Occlum.json
    else
        sed -i "s/SGX_HEAP/${SGX_HEAP}/g" Occlum.json
    fi

    if [[ -z "$SGX_KERNEL_HEAP" ]]; then
        sed -i "s/SGX_KERNEL_HEAP/1GB/g" Occlum.json
    else
        sed -i "s/SGX_KERNEL_HEAP/${SGX_KERNEL_HEAP}/g" Occlum.json
    fi
}

build_spark() {
    # Copy JVM and class file into Occlum instance and build
    cd /opt/occlum_spark
    mkdir -p image/usr/lib/jvm
    cp -r /usr/lib/jvm/java-11-openjdk-amd64 image/usr/lib/jvm
    cp -rf /etc/java-11-openjdk image/etc/
    # Copy K8s secret
    mkdir -p image/var/run/secrets/
    cp -r /var/run/secrets/* image/var/run/secrets/
    ls image/var/run/secrets/kubernetes.io/serviceaccount/
    # Copy libs
    cp /lib/x86_64-linux-gnu/libz.so.1 image/lib
    cp /lib/x86_64-linux-gnu/libz.so.1 image/$occlum_glibc
    cp /lib/x86_64-linux-gnu/libtinfo.so.5 image/$occlum_glibc
    cp /lib/x86_64-linux-gnu/libnss*.so.2 image/$occlum_glibc
    cp /lib/x86_64-linux-gnu/libresolv.so.2 image/$occlum_glibc
    cp $occlum_glibc/libdl.so.2 image/$occlum_glibc
    cp $occlum_glibc/librt.so.1 image/$occlum_glibc
    cp $occlum_glibc/libm.so.6 image/$occlum_glibc
    # Copy libhadoop
    cp /opt/libhadoop.so image/lib
    # Prepare Spark
    mkdir -p image/opt/spark
    cp -rf $SPARK_HOME/* image/opt/spark/
    # Copy etc files
    cp -rf /etc/hosts image/etc/
    echo "$HOST_IP occlum-node" >> image/etc/hosts
    # cat image/etc/hosts

    cp -rf /etc/hostname image/etc/
    cp -rf /etc/ssl image/etc/
    cp -rf /etc/passwd image/etc/
    cp -rf /etc/group image/etc/
    cp -rf /etc/nsswitch.conf image/etc/

    # Prepare XGBoost And Spark
    mkdir -p image/bin/jars
    cp -f /opt/xgboostsparksgx-1.0-SNAPSHOT-jar-with-dependencies.jar image/bin/jars
    cp -rf /opt/spark-source image/opt/
    occlum build
}

run_spark_pi() {
    init_instance spark
    build_spark
    echo -e "${BLUE}occlum run spark Pi${NC}"
    occlum run /usr/lib/jvm/java-11-openjdk-amd64/bin/java \
                -XX:-UseCompressedOops -XX:MaxMetaspaceSize=$META_SPACE \
                -XX:ActiveProcessorCount=4 \
                -Divy.home="/tmp/.ivy" \
                -Dos.name="Linux" \
                -cp "$SPARK_HOME/conf/:$SPARK_HOME/jars/*" \
                -Xmx10g org.apache.spark.deploy.SparkSubmit \
                --jars $SPARK_HOME/examples/jars/spark-examples_2.12-3.1.2.jar,$SPARK_HOME/examples/jars/scopt_2.12-3.7.1.jar \
                --class org.apache.spark.examples.SparkPi spark-internal
}

run_spark_xgboost() {
    init_instance spark
    build_spark
    echo -e "${BLUE}occlum run BigDL Spark XGBoost${NC}"

    occlum run /usr/lib/jvm/java-11-openjdk-amd64/bin/java \
                -XX:-UseCompressedOops -XX:MaxMetaspaceSize=1024m \
                -XX:ActiveProcessorCount=8 \
                -Divy.home="/tmp/.ivy" \
                -Dos.name="Linux" \
                -cp "$SPARK_HOME/conf/:$SPARK_HOME/jars/*:/bin/jars/*" \
                -Xmx24g -Xms24g org.apache.spark.deploy.SparkSubmit \
                --master local[16] \
                --conf spark.task.cpus=4 \
                --conf spark.task.maxFailures=8 \
                --class xgboostsparksgx.xgbClassifierTrainingExample \
                --num-executors 2 \
                --executor-cores 4 \
                --executor-memory 10G \
                --driver-memory 4G \
                /bin/jars/xgboostsparksgx-1.0-SNAPSHOT-jar-with-dependencies.jar \
                /host/data/xgboost 2 /host/data/model LDlxjm0y3HdGFniIGviJnMJbmFI+lt3dfIVyPJm1YSY= 1
}


id=$([ -f "$pid" ] && echo $(wc -l < "$pid") || echo "0")

arg=$1
case "$arg" in
    init)
        init_instance
        build_spark
        ;;
    pi)
        run_spark_pi
        cd ../
        ;;
    xgboost)
        run_spark_xgboost
        cd ../
        ;;
esac