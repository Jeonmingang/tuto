package com.minkang.tuto.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Trigger {
    public final String worldName;
    public final UUID worldUid;
    public final double x,y,z;
    public final double radius;
    public final List<String> commands;

    public Trigger(String worldName, UUID worldUid, double x, double y, double z, double radius, List<String> commands) {
        this.worldName = worldName;
        this.worldUid = worldUid;
        this.x = x; this.y = y; this.z = z;
        this.radius = radius;
        this.commands = commands != null ? new ArrayList<>(commands) : new ArrayList<>();
    }
}