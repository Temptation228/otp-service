
## 📋 Основные функции

- **Регистрация и аутентификация пользователей** с ролями: `ADMIN` и `USER`
- **Генерация и отправка OTP-кодов**:
- **Проверка OTP-кодов** с учетом статусов
- **Администрирование**
- **Токенная авторизация** не jwt 
- **Логирование** всех операций

---

## Технологии

- **Java 17**
- **PostgreSQL + JDBC** 
- **Maven** 
- **JavaMail** 
- **SMPP** 
- **Telegram Bot API**
- **com.sun.net.httpserver**
- **SLF4J/Logback**

---

## Установка и запуск

Создайте базу данных `otp_service`:

```sql
CREATE DATABASE otp_service;
```

Заполните конфигурационные файлы в `src/main/resources`:

- `application.properties` (параметры БД)
- `email.properties` (SMTP сервер)
- `sms.properties` (SMPP эмулятор)
- `telegram.properties` (токен и chatId)

### Сборка и запуск

Соберите проект и запустите приложение:

```bash
mvn clean package
java -jar target/otp-backend.jar
```

---

## Роли и авторизация

- **ADMIN**: полные права управления
  - настройка OTP
  - просмотр и удаление пользователей
- **USER**: ограниченные права
  - генерация и валидация OTP

### Токены

- Генерируются при логине, имеют ограниченный TTL
- Передаются в заголовке:

```http
Authorization: Bearer <token>
```

---

## Примеры API-запросов

### Регистрация 

```bash
curl -X POST http://localhost:8080/register \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"password123","role":"USER"}'
```

### Вход 

```bash
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"password123"}'
```

### Генерация OTP

```bash
curl -X POST http://localhost:8080/pass/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"operationId":"op123","channel":"EMAIL"}'
```

### Проверка OTP

```bash
curl -X POST http://localhost:8080/pass/validate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"code":"123456"}'
```

### Действия администратора

```bash
# Изменение параметров OTP
curl -X PATCH http://localhost:8080/admin/config \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -d '{"length":6,"ttlSeconds":300}'

# Просмотр пользователей
curl -X GET http://localhost:8080/admin/users \
  -H "Authorization: Bearer ADMIN_TOKEN"

# Удаление пользователя
curl -X DELETE http://localhost:8080/admin/users/1 \
  -H "Authorization: Bearer ADMIN_TOKEN"
```


