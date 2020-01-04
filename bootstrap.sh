#!/usr/bin/env bash

trap "docker-compose down && yes | docker system prune && yes | docker volume prune" SIGINT

docker-compose up
