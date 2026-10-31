package com.domotiq.controller;

import com.domotiq.repository.EnergyRepository;
import com.domotiq.repository.EnergyRepository.DailyTotal;
import com.domotiq.repository.EnergyRepository.DeviceTotal;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Aggregations for the energy dashboard (estimates from simulated uptime). */
public class EnergyController {

    private final EnergyRepository repository = new EnergyRepository();

    public EnergyRepository getRepository() {
        return repository;
    }

    /** The last `days` calendar days (oldest first), zero-filled. */
    public List<DailyTotal> lastDays(int userId, int days) throws SQLException {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(days - 1L);
        Map<LocalDate, Double> found = new HashMap<>();
        for (DailyTotal t : repository.dailyTotals(userId, from, today)) {
            found.put(t.date(), t.kwh());
        }
        List<DailyTotal> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = from.plusDays(i);
            result.add(new DailyTotal(d, found.getOrDefault(d, 0.0)));
        }
        return result;
    }

    public DeviceTotal[] monthByDevice(int userId) throws SQLException {
        LocalDate first = LocalDate.now().withDayOfMonth(1);
        return repository.deviceTotals(userId, first, LocalDate.now());
    }

    public double monthTotal(int userId) throws SQLException {
        LocalDate first = LocalDate.now().withDayOfMonth(1);
        return repository.totalBetween(userId, first, LocalDate.now());
    }

    public double todayTotal(int userId) throws SQLException {
        LocalDate today = LocalDate.now();
        return repository.totalBetween(userId, today, today);
    }
}
