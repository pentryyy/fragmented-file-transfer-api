@REM Для file-upload-topic:
docker exec kafka kafka-topics ^
    --create ^
    --bootstrap-server localhost:9092 ^
    --topic file-upload-topic ^
    --partitions 1 ^
    --replication-factor 1 ^
    --config cleanup.policy=compact

@REM Для file-chunks-topic
docker exec kafka kafka-topics ^
    --create ^
    --bootstrap-server localhost:9092 ^
    --topic file-chunks-topic ^
    --partitions 3 ^
    --replication-factor 1 ^
    --config min.insync.replicas=1

@REM Для file-assemble-topic
docker exec kafka kafka-topics ^
    --create ^
    --bootstrap-server localhost:9092 ^
    --topic file-assemble-topic ^
    --partitions 1 ^
    --replication-factor 1