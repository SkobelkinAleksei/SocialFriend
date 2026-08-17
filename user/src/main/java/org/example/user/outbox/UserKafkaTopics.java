package org.example.user.outbox;

public final class UserKafkaTopics {

    public static final String REGISTERED = "user-registered";
    public static final String EMAIL_UPDATED = "user-email-updated";
    public static final String PASSWORD_UPDATED = "user-password-updated";
    public static final String SETTINGS_UPDATED = "user-settings-updated-topic";
    public static final String ACCOUNT_STATUS_CHANGED = "user-account-status-changed";
    public static final String PLATFORM_ROLE_CHANGED = "user-platform-role-changed";

    private UserKafkaTopics() {
    }
}
