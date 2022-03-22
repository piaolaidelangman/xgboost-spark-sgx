export HTTP_PROXY_HOST=
export HTTP_PROXY_PORT=
export HTTPS_PROXY_HOST=
export HTTPS_PROXY_PORT=
export $SPARK_JAR_REPO_URL=

sudo docker build \
    --build-arg http_proxy=http://$HTTP_PROXY_HOST:$HTTP_PROXY_PORT \
    --build-arg https_proxy=http://$HTTPS_PROXY_HOST:$HTTPS_PROXY_PORT \
    --build-arg HTTP_PROXY_HOST=$HTTP_PROXY_HOST \
    --build-arg HTTP_PROXY_PORT=$HTTP_PROXY_PORT \
    --build-arg HTTPS_PROXY_HOST=$HTTPS_PROXY_HOST \
    --build-arg HTTPS_PROXY_PORT=$HTTPS_PROXY_PORT \
    --build-arg no_proxy=x.x.x.x \
    --build-arg SPARK_JAR_REPO_URL=$SPARK_JAR_REPO_URL \
    -t xgboost-spark-sgx:1.0.0 -f ./Dockerfile .
