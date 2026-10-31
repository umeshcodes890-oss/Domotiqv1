package com.domotiq.controller;

import com.domotiq.model.ActivityLog;
import com.domotiq.repository.ActivityLogRepository;

import java.sql.SQLException;
import java.util.List;

public class ActivityController {

    private final ActivityLogRepository repository = new ActivityLogRepository();

    public List<ActivityLog> recent(int userId, int limit) throws SQLException {
        return repository.findByUser(userId, Math.max(1, limit));
    }
}
