# Entity Area Logger

This is a simple client side fabric mod that lets users log the number of entities by type in a certain area into a csv file 2 times per second.
Users can define blocks which trigger the logger when the block state is updated.

## Commands

The logger can be accessed via the `/logentities` command. The mod provides 4 principle commands:

Starting the logger:
```
/logentities start x1 y1 z1 x2 y2 z2
```

Stopping the logger:
```
/logentities stop
```

Setting up the logger with a trigger:
```
/logentities trigger x1 y1 z1 x2 y2 z2 on x y z
```
Starts/stops logging entities in (x1, y1, z1) to (x2, y2, z2) when (x, y, z) gets a block update.

Clearing the logger:
```
/logentities trigger clear
```
Stops the logger and clears the triggers set.

## Datastructure
The logger writes a file into the subdirectory `entity_logs` in your installation directory.
The csv file has 4 fields:
field | description
------|-------------
tick        | Ticks since logger was started.
world_time  | World time in ticks.
entity_type | Entity which was counted.
count       | Number of entities of type.

Every 10 ticks the number of entities of each type is counted in the area and each non-zero count is written to the file.