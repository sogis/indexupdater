#!/bin/bash

echo "=========================================================="
echo "Docker container for test PostgreSQL database with postgis"
echo "Uses the following Docker image:"
echo "https://hub.docker.com/r/mdillon/postgis"
echo "=========================================================="

docker run -it \
    --name pgdev \
    -v $(pwd)/pg_initfiles:/docker-entrypoint-initdb.d \
    -p 5432:5432 \
    --rm \
    mdillon/postgis:9.6
