@REM Для file-processing-topic:
docker exec kafka kafka-topics ^
    --create ^
    --bootstrap-server localhost:9092 ^
    --topic file-processing-topic ^
    --partitions 1 ^
    --replication-factor 1 ^