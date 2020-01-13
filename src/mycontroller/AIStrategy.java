package mycontroller;

import tiles.MapTile;
import world.WorldSpatial;
/**
 * Interface to use strategy pattern
 */
public interface AIStrategy {
	WorldSpatial.Direction Directing(WorldSpatial.Direction orientation);
}
