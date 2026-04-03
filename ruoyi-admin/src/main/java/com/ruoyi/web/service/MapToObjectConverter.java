package com.ruoyi.web.service;

import java.util.Map;

public interface MapToObjectConverter {
    Object convert(Map<String, Object> sourceMap);
}
