#!/bin/bash

# Compile all Java files
echo "Compiling Java files..."
javac Annuaire/*.java Client/*.java
if [ $? -ne 0 ]; then
  echo "Compilation failed. Please fix the errors and try again."
  exit 1
fi
echo "Compilation successful."

# Create an array to store PIDs of xterm windows
xterm_pids=()

# Run AnnuaireImpl in a new terminal
xterm -hold -e "java Annuaire.AnnuaireImpl" &
xterm_pids+=($!)
echo "AnnuaireImpl started with PID ${xterm_pids[-1]}."

while ! nc -z localhost 9008; do
    sleep 1
done
echo "Daemon oussama is ready."

# Start Daemons in new terminals
xterm -hold -e "java Client.Client oussama 8080 0" &
xterm_pids+=($!)
echo "Daemon oussama started on port 8080 with PID ${xterm_pids[-1]}."
sleep 2 

xterm -hold -e "java Client.Client hlima 8081 0" &
xterm_pids+=($!)
echo "Daemon hlima started on port 8081 with PID ${xterm_pids[-1]}."
sleep 2

# Start Downloader in a new terminal
xterm -hold -e "java Client.Client abdellatif 8082 1 BigBuckBunny.mp4" &
xterm_pids+=($!)
echo "Downloader abdellatif started on port 8082 with PID ${xterm_pids[-1]}."

xterm -hold -e "java Client.Client imran 8083 1 BigBuckBunny.mp4" &
xterm_pids+=($!)
echo "Downloader imran started on port 8083 with PID ${xterm_pids[-1]}."

xterm -hold -e "java Client.Client jillali 8084 1 BigBuckBunny.mp4" &
xterm_pids+=($!)
echo "Downloader jillali started on port 8084 with PID ${xterm_pids[-1]}."

echo "All services are running. Press Ctrl+C to stop the script."


# Keep the script running
while true; do
    sleep 1
done
