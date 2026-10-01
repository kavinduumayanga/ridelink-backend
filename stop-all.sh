#!/usr/bin/env bash

set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd -P)"
RUN_DIR="$ROOT_DIR/.run"
PID_FILE="$RUN_DIR/services.pids"

if [[ ! -f "$PID_FILE" ]]; then
    echo "No RideLink processes recorded by start-all.sh."
    exit 0
fi

process_marker() {
    local pid="$1"
    ps -p "$pid" -o lstart= 2>/dev/null \
        | sed 's/^[[:space:]]*//; s/[[:space:]]*$//'
}

target_pids=()
target_markers=()

target_exists() {
    local candidate="$1"
    local index
    for ((index = 0; index < ${#target_pids[@]}; index++)); do
        [[ "${target_pids[$index]}" == "$candidate" ]] && return 0
    done
    return 1
}

capture_target() {
    local pid="$1"
    local marker

    target_exists "$pid" && return 0
    marker="$(process_marker "$pid")"
    [[ -n "$marker" ]] || return 0

    target_pids+=("$pid")
    target_markers+=("$marker")
}

capture_process_tree() {
    local parent_pid="$1"
    local child_pid

    while read -r child_pid; do
        [[ -n "$child_pid" ]] || continue
        capture_process_tree "$child_pid"
    done < <(ps -eo pid=,ppid= 2>/dev/null \
        | awk -v parent="$parent_pid" '$2 == parent { print $1 }')

    capture_target "$parent_pid"
}

process_still_matches() {
    local pid="$1"
    local expected_marker="$2"
    kill -0 "$pid" 2>/dev/null \
        && [[ "$(process_marker "$pid")" == "$expected_marker" ]]
}

recorded_services=0
while IFS='|' read -r service pid marker port log_path; do
    [[ -n "$service" && -n "$pid" && -n "$marker" ]] || continue
    recorded_services=$((recorded_services + 1))

    if process_still_matches "$pid" "$marker"; then
        echo "Stopping $service (PID $pid)..."
        capture_process_tree "$pid"
    else
        echo "Ignoring stale PID metadata for $service (PID $pid)."
    fi
done < "$PID_FILE"

if (( recorded_services == 0 )); then
    echo "No valid process records were found. Removing stale metadata."
    rm -f "$PID_FILE"
    rmdir "$RUN_DIR" 2>/dev/null || true
    exit 0
fi

if (( ${#target_pids[@]} == 0 )); then
    rm -f "$PID_FILE"
    rmdir "$RUN_DIR" 2>/dev/null || true
    echo "No recorded RideLink processes are still running. Stale metadata removed."
    exit 0
fi

for ((index = 0; index < ${#target_pids[@]}; index++)); do
    if process_still_matches "${target_pids[$index]}" "${target_markers[$index]}"; then
        kill -TERM "${target_pids[$index]}" 2>/dev/null || true
    fi
done

for ((attempt = 0; attempt < 20; attempt++)); do
    any_running=false
    for ((index = 0; index < ${#target_pids[@]}; index++)); do
        if process_still_matches "${target_pids[$index]}" "${target_markers[$index]}"; then
            any_running=true
            break
        fi
    done

    [[ "$any_running" == false ]] && break
    sleep 0.5
done

for ((index = 0; index < ${#target_pids[@]}; index++)); do
    if process_still_matches "${target_pids[$index]}" "${target_markers[$index]}"; then
        echo "Process ${target_pids[$index]} did not stop gracefully; sending SIGKILL." >&2
        kill -KILL "${target_pids[$index]}" 2>/dev/null || true
    fi
done

sleep 0.5
remaining=false
for ((index = 0; index < ${#target_pids[@]}; index++)); do
    if process_still_matches "${target_pids[$index]}" "${target_markers[$index]}"; then
        remaining=true
        echo "Error: recorded process ${target_pids[$index]} is still running." >&2
    fi
done

if [[ "$remaining" == true ]]; then
    echo "PID metadata was retained for another cleanup attempt." >&2
    exit 1
fi

rm -f "$PID_FILE"
rmdir "$RUN_DIR" 2>/dev/null || true
echo "All RideLink processes recorded by start-all.sh have stopped."
