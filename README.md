# IVR Service

This repo contains the LabourConnect IVR service.

## Docker & Compose

Two recommended ways to run:

1) Build image and run on the GCP VM host (connect to other services via host-mapped ports)

```powershell
# build
docker build -t smileyishere1008/ivr-service:latest .

# run
docker run -d --name ivr-service --network prohands -p 8089:8089 \
  -e POSTGRES_HOST=postgres -e POSTGRES_PORT=5432 -e POSTGRES_DB=ivr \
  -e POSTGRES_USER=anand -e POSTGRES_PASSWORD=1008 \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  -e SPRING_PROFILES_ACTIVE=docker \
  smileyishere1008/ivr-service:latest
```

2) Use `docker-compose` (assumes an external network named `prohands` is already created and other services are running on it):

```bash
# build and start
docker-compose up -d --build

# view logs
docker-compose logs -f ivr-service
```

Notes:
- Use `SPRING_PROFILES_ACTIVE=docker` to make the app use container hostnames `postgres` and `kafka` from the Docker network.
- If running the app on the VM host (not inside Docker), ensure Kafka and Postgres are reachable via the host's mapped ports and set env vars accordingly.

