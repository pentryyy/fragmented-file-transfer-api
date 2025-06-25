# О проекте

Предсавлены модули для разделения файлов на фрагменты и сбора файлов из полученных фрагментов. 
Модуль разделения имеет возможность гарантированно дать ответ, получен ли полностью файл вторым модулем,
если нет, какие фрагменты не были получены, и перепосылать требуемые фрагменты до тех пор, пока файл не будет полностью собран.

#  Документация Swagger

- JSON: `http://localhost:8080/v3/api-docs`

- YAML: `http://localhost:8080/v3/api-docs.yaml`

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

# Kafka

Запускается через докер образ в директории `/kafka`.  
Топики необходимо создать используя команды:

 Топик для хранения чанков.
 
```
docker exec kafka kafka-topics ^
  --create ^
  --bootstrap-server localhost:9092 ^
  --topic file-chunks ^
  --partitions 3 ^
  --replication-factor 1
```

 Топик для хранения обратной связи.
 
```
docker exec kafka kafka-topics ^
  --create ^
  --bootstrap-server localhost:9092 ^
  --topic file-feedbacks ^
  --partitions 3 ^
  --replication-factor 1
```


Команда для просмотра всех топиков:

```
docker exec kafka kafka-topics ^
  --list ^
  --bootstrap-server localhost:9092      
```