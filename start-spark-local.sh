sudo docker rm -f xgboost-spark-sgx

sudo docker run -it \
	--privileged \
	--net=host \
	--name=xgboost-spark-sgx \
	--cpuset-cpus 10-14 \
	--device=/dev/sgx/enclave \
	--device=/dev/sgx/provision \
	-v /var/run/aesmd/aesm.socket:/var/run/aesmd/aesm.socket \
    -v data:/opt/occlum_spark/data \
	-e LOCAL_IP=Your_IP \
	-e SGX_MEM_SIZE=24GB \
	xgboost-spark-sgx:1.0 \
	bash /opt/run_spark_on_occlum_glibc.sh $1 && tail -f /dev/null
