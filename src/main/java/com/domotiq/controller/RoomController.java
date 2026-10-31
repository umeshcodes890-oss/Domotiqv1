package com.domotiq.controller;

import com.domotiq.model.Room;
import com.domotiq.repository.RoomRepository;

import java.sql.SQLException;
import java.util.List;

public class RoomController {

    private final RoomRepository roomRepository = new RoomRepository();

    public List<Room> listRooms(int userId) throws SQLException {
        return roomRepository.findByUser(userId);
    }

    public Room addRoom(int userId, String name, String description) throws SQLException {
        String clean = requireName(name);
        if (roomRepository.nameTaken(userId, clean, -1)) {
            throw new IllegalArgumentException("You already have a room named \"" + clean + "\".");
        }
        return roomRepository.insert(userId, clean, blankToNull(description));
    }

    public void updateRoom(Room room) throws SQLException {
        room.setName(requireName(room.getName()));
        if (roomRepository.nameTaken(room.getUserId(), room.getName(), room.getId())) {
            throw new IllegalArgumentException(
                        "You already have another room named \"" + room.getName() + "\".");
        }
        room.setDescription(blankToNull(room.getDescription()));
        roomRepository.update(room);
    }

    /**
     * Devices assigned to the room become unassigned (FK ON DELETE SET NULL),
     * they are never deleted along with the room.
     */
    public void deleteRoom(Room room) throws SQLException {
        roomRepository.delete(room.getId(), room.getUserId());
    }

    private String requireName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Room name is required.");
        }
        return name.trim();
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
