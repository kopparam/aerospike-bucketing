# Aerospike with Bucketing data model

This project shows how to store data in Aerospike
with bucketing data model.

The data we will store is a simple `User` object with `id` and `data` fields.

However, it is a good idea to keep that id private within your database, 
and use a different id for integrating with other systems. These IDs used for
external integrations will be known as external IDs.

The bucketing data model will allow us to query the data of a User using
the internal of any of its external IDs.

## Getting Started

### Setup

```shell
# Start Aerospike
docker-compose up -d

# Run the application
./gradlew bootRun
```

### Usage

```shell
# Create a user
curl -X POST localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"externalId": "123", "data": "some data"}'
# {"id":"1","externalId":"123","data":"some data"}

# Get a user by internal id
curl localhost:8080/users/1
# {"id":"1","externalId":"123","data":"some data"}

# Get a user by external id
curl localhost:8080/users/external/123
# {"id":"1","externalId":"123","data":"some data"}
```
