FROM 10.239.45.10/arda/intelanalytics/bigdl-ppml-trusted-big-data-ml-scala-occlum:2.1.0-SNAPSHOT
ADD ./log4j2.xml /opt/spark/conf/log4j2.xml

RUN sed -i "s/log4j.rootCategory=INFO, console/log4j.rootCategory=DEBUG, console/g" /opt/spark/conf/log4j.properties && \
    sed -i "s/log4j.logger.io.netty=ERROR/log4j.logger.io.netty=DEBUG/g" /opt/spark/conf/log4j.properties && \
    cat /opt/spark/conf/log4j.properties

COPY ./entrypoint.sh /opt/

RUN chmod a+x /opt/entrypoint.sh

ENTRYPOINT [ "/opt/entrypoint.sh" ]
