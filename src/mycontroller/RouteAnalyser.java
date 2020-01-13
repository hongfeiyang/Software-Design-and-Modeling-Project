package mycontroller;

import java.util.HashMap;

import mycontroller.strategy.AIStrategy;
import mycontroller.strategy.ExploringStrategy;
import mycontroller.strategy.FindingPathStrategy;
import mycontroller.strategy.DrivingAlongWall;
import mycontroller.strategy.DrivingAlongWallnLava;
import tiles.MapTile;
import utilities.Coordinate;
import world.WorldSpatial;

/**
 * Analyse tiles around us en route
 */
public class RouteAnalyser {
	//private boolean WallOrTrapAround;
	private AIStrategy currentStrategy;	// strategy we are currently using
	//private FindingPathStrategy findingpathstrategy;
	
	public RouteAnalyser() {
		currentStrategy = DrivingAlongWallnLava.getDrivingAlongWallnLavaInstance();	}
	
	/**
	 * To explore the map
	 * @param map	internal representation of the map
	 * @param currentView	current tiles we can see
	 * @param orientation	orientation the car is facing
	 * @param carposition	coordinate the car is at
	 * @return
	 */
	public boolean ExploreMap(HashMap<Coordinate, MapTile> map,HashMap<Coordinate, MapTile> currentView,WorldSpatial.Direction orientation,Coordinate carposition) {
		return ((ExploringStrategy)currentStrategy).checkNorth(currentView,carposition);
	}
	
	/**
	 * change strategy
	 * 
	 * @param strategy: ID to identify the strategy we are going to use
	 */
	public void SwitchStrategy(String strategy) {
		if(strategy.equals("WALL")) {
			currentStrategy = DrivingAlongWall.getDrivingAlongWallInstance();
		}else if(strategy.equals("WALL&LAVA")){
			currentStrategy = DrivingAlongWallnLava.getDrivingAlongWallnLavaInstance();
		}
	}
	
	/**
	 * Check if you have a wall in front of you!
	 * @param orientation the orientation we are in based on WorldSpatial
	 * @param currentView what the car can currently see
	 * @return true if wall is ahead, false otherwise
	 */
	public boolean checkWallAhead(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView,Coordinate carposition){
		switch(orientation){
		case EAST:
			return ((ExploringStrategy)currentStrategy).checkEast(currentView,carposition);
		case NORTH:
			return ((ExploringStrategy)currentStrategy).checkNorth(currentView,carposition);
		case SOUTH:
			return ((ExploringStrategy)currentStrategy).checkSouth(currentView,carposition);
		case WEST:
			return ((ExploringStrategy)currentStrategy).checkWest(currentView,carposition);
		default:
			return false;
		}
	}
	
	/**
	 * Check if the wall is on your left hand side given your orientation
	 * @param orientation
	 * @param currentView
	 * @return ture if is following fall, false otherwise
	 */
	public boolean checkFollowingWall(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView,Coordinate carposition) {
		switch(orientation){
		case EAST:
			return ((ExploringStrategy)currentStrategy).checkNorth(currentView,carposition);
		case NORTH:
			return ((ExploringStrategy)currentStrategy).checkWest(currentView,carposition);
		case SOUTH:
			return ((ExploringStrategy)currentStrategy).checkEast(currentView,carposition);
		case WEST:
			return ((ExploringStrategy)currentStrategy).checkSouth(currentView,carposition);
		default:
			return false;
		}
		
	}
}
