#!/usr/bin/env bash

set -Eeo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd -P)"
ENV_FILE="$ROOT_DIR/.env"
RUN_DIR="$ROOT_DIR/.run"
PID_FILE="$RUN_DIR/services.pids"
LOG_DIR="$ROOT_DIR/logs"
STOP_SCRIPT="$ROOT_DIR/stop-all.sh"
STARTUP_TIMEOUT_SECONDS=120

if [[ ! -f "$ENV_FILE" ]]; then
    echo "Error: $ENV_FILE does not exist." >&2
    echo "Copy $ROOT_DIR/.env.example to $ENV_FILE and configure it before starting RideLink." >&2
    exit 1
fi

set -a
if ! source "$ENV_FILE"; then
    set +a
    echo "Error: failed to load $ENV_FILE. Check its shell syntax and values." >&2
    exit 1
fi
set +a
set -u

required_variables=(
    ACCOUNT_MONGODB_URI
    DRIVER_MONGODB_URI
    RIDE_MONGODB_URI
    PAYMENT_MONGODB_URI
    JWT_SECRET
    DRIVER_SERVICE_URL
    FARE_PAYMENT_SERVICE_URL
)

missing_variables=()
for variable in "${required_variables[@]}"; do
    if [[ -z "${!variable:-}" ]]; then
        missing_variables+=("$variable")
    fi
done

if (( ${#missing_variables[@]} > 0 )); then
    echo "Error: required environment variables are missing or empty:" >&2
    for variable in "${missing_variables[@]}"; do
        echo "  - $variable" >&2
    done
    echo "Configure them in $ENV_FILE before starting RideLink." >&2
    exit 1
fi

jwt_secret_bytes="$(LC_ALL=C printf '%s' "$JWT_SECRET" | wc -c | tr -d '[:space:]')"
if (( jwt_secret_bytes < 32 )); then
    echo "Error: JWT_SECRET must contain at least 32 bytes for HS256." >&2
    exit 1
fi

for command_name in java mvn; do
    if ! command -v "$command_name" >/dev/null 2>&1; then
        echo "Error: required command '$command_name' was not found in PATH." >&2
        exit 1
    fi
done

java_version_output="$(java -version 2>&1)"
maven_version_output="$(mvn -version 2>&1)"
echo "Java: ${java_version_output%%$'\n'*}"
echo "Maven: ${maven_version_output%%$'\n'*}"

port_is_listening() {
    local port="$1"

    if command -v lsof >/dev/null 2>&1; then
        lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1
    elif command -v nc >/dev/null 2>&1; then
        nc -z 127.0.0.1 "$port" >/dev/null 2>&1
    else
        (exec 3<>"/dev/tcp/127.0.0.1/$port") >/dev/null 2>&1
    fi
}

occupied_ports=()
for port in 8081 8082 8083 8084; do
    if port_is_listening "$port"; then
        occupied_ports+=("$port")
    fi
done

if (( ${#occupied_ports[@]} > 0 )); then
    echo "Error: required ports are already occupied:" >&2
    for port in "${occupied_ports[@]}"; do
        echo "  - $port" >&2
    done
    echo "No processes were stopped. Free these ports and run the launcher again." >&2
    exit 1
fi

process_marker() {
    local pid="$1"
    ps -p "$pid" -o lstart= 2>/dev/null \
        | sed 's/^[[:space:]]*//; s/[[:space:]]*$//'
}

if [[ -f "$PID_FILE" ]]; then
    active_record=false
    while IFS='|' read -r recorded_service recorded_pid recorded_marker recorded_port recorded_log; do
        [[ -n "$recorded_pid" && -n "$recorded_marker" ]] || continue
        if kill -0 "$recorded_pid" 2>/dev/null \
                && [[ "$(process_marker "$recorded_pid")" == "$recorded_marker" ]]; then
            active_record=true
            echo "Error: recorded RideLink process '$recorded_service' is still running with PID $recorded_pid." >&2
        fi
    done < "$PID_FILE"

    if [[ "$active_record" == true ]]; then
        echo "Run ./stop-all.sh before starting another launcher instance." >&2
        exit 1
    fi

    rm -f "$PID_FILE"
fi

mkdir -p "$RUN_DIR" "$LOG_DIR"
: > "$PID_FILE"

service_names=()
service_slugs=()
service_ports=()
service_pids=()
service_logs=()
service_ready=()

cleanup_on_exit() {
    local status=$?
    trap - EXIT INT TERM HUP
    if [[ -f "$PID_FILE" ]]; then
        bash "$STOP_SCRIPT" || status=1
    fi
    exit "$status"
}

trap cleanup_on_exit EXIT
trap 'echo; echo "Shutdown requested."; exit 130' INT
trap 'echo; echo "Termination requested."; exit 143' TERM
trap 'echo; echo "Terminal closed."; exit 129' HUP

start_service() {
    local display_name="$1"
    local slug="$2"
    local service_directory="$3"
    local port="$4"
    local log_relative="logs/$slug.log"
    local log_absolute="$ROOT_DIR/$log_relative"
    local pid
    local marker

    : > "$log_absolute"
    (
        cd "$ROOT_DIR/$service_directory"
        exec mvn spring-boot:run
    ) >>"$log_absolute" 2>&1 &
    pid=$!
    marker="$(process_marker "$pid")"

    if [[ -z "$marker" ]]; then
        echo "Error: $display_name exited before its process metadata could be recorded." >&2
        echo "See $log_relative" >&2
        return 1
    fi

    printf '%s|%s|%s|%s|%s\n' \
        "$slug" "$pid" "$marker" "$port" "$log_relative" >> "$PID_FILE"

    service_names+=("$display_name")
    service_slugs+=("$slug")
    service_ports+=("$port")
    service_pids+=("$pid")
    service_logs+=("$log_relative")
    service_ready+=("false")

    printf '%-22s -> http://localhost:%s\n' "$display_name" "$port"
    printf '%-22s    %s\n' "Log" "$log_relative"
}

echo
echo "Starting RideLink services from $ROOT_DIR"
echo

start_service "Account Service" "account-service" "account-service" "8081"
start_service "Driver Service" "driver-service" "driver-service" "8082"
start_service "Ride Service" "ride-service" "ride-service" "8083"
start_service "Fare/Payment Service" "fare-payment-service" "fare-payment-service" "8084"

echo
echo "Waiting up to ${STARTUP_TIMEOUT_SECONDS}s for all service ports..."

deadline=$((SECONDS + STARTUP_TIMEOUT_SECONDS))
while true; do
    all_ready=true

    for ((index = 0; index < ${#service_pids[@]}; index++)); do
        if [[ "${service_ready[$index]}" == true ]]; then
            continue
        fi

        all_ready=false
        pid="${service_pids[$index]}"
        if ! kill -0 "$pid" 2>/dev/null; then
            echo "Error: ${service_names[$index]} exited during startup." >&2
            echo "See ${service_logs[$index]}" >&2
            exit 1
        fi

        if port_is_listening "${service_ports[$index]}"; then
            service_ready[$index]=true
            echo "Ready: ${service_names[$index]} on port ${service_ports[$index]}"
        fi
    done

    if [[ "$all_ready" == true ]]; then
        break
    fi

    if (( SECONDS >= deadline )); then
        echo "Error: RideLink startup timed out after ${STARTUP_TIMEOUT_SECONDS}s." >&2
        for ((index = 0; index < ${#service_pids[@]}; index++)); do
            if [[ "${service_ready[$index]}" != true ]]; then
                echo "  - ${service_names[$index]}: see ${service_logs[$index]}" >&2
            fi
        done
        exit 1
    fi

    sleep 1
done

echo
echo "================================================"
echo "RideLink is running"
echo "================================================"
printf '%-22s http://localhost:8081\n' "Account Service"
printf '%-22s http://localhost:8082\n' "Driver Service"
printf '%-22s http://localhost:8083\n' "Ride Service"
printf '%-22s http://localhost:8084\n' "Fare/Payment Service"
echo
echo "Logs:"
echo "logs/account-service.log"
echo "logs/driver-service.log"
echo "logs/ride-service.log"
echo "logs/fare-payment-service.log"
echo
echo "Press Ctrl+C to stop all services."

while true; do
    for ((index = 0; index < ${#service_pids[@]}; index++)); do
        if ! kill -0 "${service_pids[$index]}" 2>/dev/null; then
            echo "Error: ${service_names[$index]} stopped unexpectedly." >&2
            echo "See ${service_logs[$index]}" >&2
            exit 1
        fi
    done
    sleep 2
done
