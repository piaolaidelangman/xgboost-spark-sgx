sudo docker rm -f xgboost-spark-sgx

sudo docker run -it \
	--privileged \
	--net=host \
	--name=xgboost-spark-sgx \
	--cpuset-cpus 76-126 \
	--device=/dev/sgx/enclave \
	--device=/dev/sgx/provision \
	-v /var/run/aesmd/aesm.socket:/var/run/aesmd/aesm.socket \
	-v data:/opt/occlum_spark/data \
	-e LOCAL_IP=$LOCAL_IP \
	-e SGX_MEM_SIZE=20GB \
	-e SGX_THREAD=512 \
	-e SGX_HEAP=4GB \
	-e SGX_KERNEL_HEAP=1GB \
	$IMAGE \
	bash /opt/run_spark_on_occlum_glibc.sh pi
	#$1 && tail -f /dev/null
