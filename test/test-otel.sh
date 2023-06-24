#!/bin/bash

names=("minifying" "processing" "logging" "caching" "authenticating" "analyzing" "optimizing" "encrypting" "decrypting")
num_names=${#names[@]}

for ((i = 0; i < 1000; i++)); do
  # Generate random trace ID and span ID
  trace_id=$(openssl rand -hex 16)
  span_id=$(openssl rand -hex 8)

  # Generate random start and end timestamps within a range
  start_time=$(shuf -i 1544712660300000000-1544712660600000000 -n 1)
  end_time=$((start_time + RANDOM % 1000000000)) # Adding random duration within 1 second

  # Select a random name from the list
  name=${names[$((RANDOM % num_names))]}

  # Construct the payload with random values
  payload='{
    "resource_spans": [
      {
        "scope_spans": [
          {
            "spans": [
              {
                "trace_id": "'"$trace_id"'",
                "span_id": "'"$span_id"'",
                "name": "'"$name"'",
                "start_time_unix_nano": '$start_time',
                "end_time_unix_nano": '$end_time'
              }
            ]
          }
        ]
      }
    ]
  }'

  # Send the payload via curl
  echo "$payload" | curl -X POST http://localhost:4318/v1/traces \
    -H 'Content-Type: application/json' \
    --data @-

  sleep 0.05
done
