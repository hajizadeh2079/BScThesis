#!/bin/bash

URL="http://localhost:8080/hello"

for ((i = 0; i < 1; i++)); do
    curl -s $URL &
    sleep 0.1
done
