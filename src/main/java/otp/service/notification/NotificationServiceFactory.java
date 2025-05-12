package otp.service.notification;

public class NotificationServiceFactory {
    public NotificationService getService(NotificationChannel channel) {
        switch (channel) {
            case EMAIL:
                return new EmailService();
            case SMS:
                return new SmsService();
            case TELEGRAM:
                return new TelegramService();
            case FILE:
                return new FileService();
            default:
                throw new IllegalArgumentException("Канал не поддерживается: " + channel);
        }
    }
}

