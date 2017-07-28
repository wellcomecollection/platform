#!/usr/bin/env sh

INSTANCE_ID=$(curl http://169.254.169.254/latest/meta-data/instance-id)

echo $INSTANCE_ID > instance_id.out

while true; do

    STATUS_CODE=$(curl \
        --write-out %{http_code} \
        --silent \
        --output body.out \
        http://169.254.169.254/latest/meta-data/spot/termination-time)

    if [[ "$STATUS_CODE" == "200" ]]; then
        echo "Sending succesful apply notification."

        TERMINATION_TIME=$(cat body.out)

        printf \
            '{"instance_id":"%s","termination_time":"%s"}\n' \
            "$INSTANCE_ID" \
            "$TERMINATION_TIME" \
            > notice.json

        /app/notify.sh spot_termination_notice notice.json

        break
    fi

    sleep 5
done