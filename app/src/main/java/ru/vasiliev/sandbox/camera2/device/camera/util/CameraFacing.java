package ru.vasiliev.sandbox.camera2.device.camera.util;

public enum CameraFacing {
    FRONT(0),
    BACK(1),
    ANY(2),
    UNKNOWN(3);

    private int id;

    CameraFacing(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static CameraFacing byId(int id) {
        for (CameraFacing facing : values()) {
            if (facing.getId() == id) {
                return facing;
            }
        }
        return UNKNOWN;
    }
}
