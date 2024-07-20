#!/bin/bash
# Convert input to lowercase
input=$(echo "$2" | tr '[:upper:]' '[:lower:]')
port=${3:-3000}  # Default port to 3000 if not provided

if [ "$input" = "server" ]; then
    java $1.server.Server $port
elif [ "$input" = "client" ]; then
    java $1.client.Client
    # In Milestone3 changes Client to ClientUI
elif [ "$input" = "ui" ]; then
	java $1.client.ClientUI
	# Milestone 3's new entry point
else
    echo "Must specify client or server"
fi
