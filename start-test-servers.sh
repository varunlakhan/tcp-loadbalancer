#!/bin/bash

echo "Starting test backend servers on ports 9001, 9002, 9003..."
echo "Press Ctrl+C to stop all servers"

# Function to start a server on a given port
start_server() {
    port=$1
    echo "Starting server on port $port..."
    while true; do
        nc -l $port < /dev/null > /dev/null 2>&1 || sleep 1
    done
}

# Start servers in background
start_server 9001 &
SERVER1_PID=$!

start_server 9002 &
SERVER2_PID=$!

start_server 9003 &
SERVER3_PID=$!

echo "Servers started! PIDs: $SERVER1_PID, $SERVER2_PID, $SERVER3_PID"
echo "Servers are running. Press Ctrl+C to stop."

# Wait for interrupt
trap "kill $SERVER1_PID $SERVER2_PID $SERVER3_PID 2>/dev/null; echo 'Servers stopped'; exit" INT TERM

# Keep script running
wait

