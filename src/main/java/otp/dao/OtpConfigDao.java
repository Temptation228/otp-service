package otp.dao;

import otp.model.OtpConfig;


public interface OtpConfigDao {
    OtpConfig getConfig();
    void updateConfig(OtpConfig config);
    void initDefaultConfigIfEmpty();
}

