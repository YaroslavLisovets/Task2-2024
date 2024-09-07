# Настройка Базы Данных 
### 1.1 Настроить соединение с существующей БД
Для этого в файле application.yaml необходимо выставить правильные данные
```yaml
database:
   url: "DATABASE_URL"
   driver: "org.postgresql.Driver"
   user: "USER"
   password: "PASS"
```
### 1.2 Развернуть с помощью Docker ранее настроенную БД
```shell
docker compose up -d
```
# Запуск приложения
```shell
./gradlew run
```
