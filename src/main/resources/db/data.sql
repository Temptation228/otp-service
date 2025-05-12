INSERT INTO otp_config (length, ttl_seconds)
VALUES (6, 300)
ON CONFLICT DO NOTHING;

INSERT INTO users (username, password_hash, role)
VALUES (
    'admin',
    'YOUR_HASHED_PASSWORD_HERE',
    'ADMIN'
) ON CONFLICT DO NOTHING;
