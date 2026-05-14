package com.orderplatform.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.orderplatform.payment.entity.SystemConfig;
import com.orderplatform.payment.mapper.SystemConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SystemConfigService extends ServiceImpl<SystemConfigMapper, SystemConfig> {

    public String getConfigValue(String configKey) {
        SystemConfig config = getOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, configKey));
        return config != null ? config.getConfigValue() : null;
    }

    public String getConfigValue(String configKey, String defaultValue) {
        String value = getConfigValue(configKey);
        return value != null ? value : defaultValue;
    }

    public Integer getConfigInt(String configKey, Integer defaultValue) {
        String value = getConfigValue(configKey);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("配置值转整数失败，key: {}, value: {}", configKey, value);
            return defaultValue;
        }
    }

    public Boolean getConfigBoolean(String configKey, Boolean defaultValue) {
        String value = getConfigValue(configKey);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public void updateConfigValue(String configKey, String configValue, String configDesc) {
        SystemConfig config = getOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, configKey));
        if (config == null) {
            config = new SystemConfig();
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            config.setConfigDesc(configDesc);
            save(config);
        } else {
            config.setConfigValue(configValue);
            if (configDesc != null) {
                config.setConfigDesc(configDesc);
            }
            updateById(config);
        }
    }

    public Map<String, String> getAllConfigMap() {
        List<SystemConfig> configs = list();
        return configs.stream()
                .collect(Collectors.toMap(SystemConfig::getConfigKey, SystemConfig::getConfigValue));
    }
}
