# Clean up old container
sudo docker rm -f bigdl-ppml-trusted-big-data-ml-scala-occlum-attestation-server

# Run new command in container
sudo docker run -it \
	--net=host \
	--name=bigdl-ppml-trusted-big-data-ml-scala-occlum-attestation-server \
	--cpuset-cpus 5-7 \
	--device=/dev/sgx/enclave \
	--device=/dev/sgx/provision \
	-v /var/run/aesmd/aesm.socket:/var/run/aesmd/aesm.socket \
	-e LOCAL_IP=$LOCAL_IP \
	-e SGX_MEM_SIZE=8GB \
	-e PCCS_URL=$PCCS_URL \
	-e ATTESTATION_SERVER_IP=$ATTESTATION_SERVER_IP \
	-e ATTESTATION_SERVER_PORT=$ATTESTATION_SERVER_PORT \
	$IMAGE \
	bash -c "cd /root/demos/remote_attestation/init_ra_flow/ && ./run_attestation_server.sh"