sudo docker run -it \
	--net=host \
	--name=xgboost-spark-sgx \
	--cpuset-cpus 10-14 \
	--device=/dev/sgx/enclave \
	--device=/dev/sgx/provision \
	-v /var/run/aesmd/aesm.socket:/var/run/aesmd/aesm.socket \
    -v /home/sdp/diankun/dbgen/zoo-tutorials/tpch-spark/dbgen:/opt/occlum_spark/data \
	-e LOCAL_IP=192.168.0.113 \
	-e SGX_MEM_SIZE=24GB \
	xgboost-spark-sgx:1.0 \
	bash /opt/run_spark_on_occlum_glibc.sh $1 && tail -f /dev/null
