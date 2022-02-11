sudo docker rm -f xgboost-spark-sgx

sudo docker run -it \
	--privileged \
	--net=host \
	--name=xgboost-spark-sgx \
	--cpuset-cpus 76-126 \
	--device=/dev/sgx/enclave \
	--device=/dev/sgx/provision \
	-v /var/run/aesmd/aesm.socket:/var/run/aesmd/aesm.socket \
    -v /home/sdp/diankun/encrypted_data_76G:/opt/occlum_spark/data \
	-e LOCAL_IP=192.168.0.111 \
	-e SGX_MEM_SIZE=90GB \
	xgboost-spark-sgx:1.0 \
	bash /opt/run_spark_on_occlum_glibc.sh $1 && tail -f /dev/null
