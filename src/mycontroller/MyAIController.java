package mycontroller;

import java.util.*;
import controller.CarController;
import world.Car;
import world.World;
import tiles.*;
import utilities.Coordinate;
import world.Car;
import world.WorldSpatial;

/**
 * Controller to automatically control the car
 */
public class MyAIController extends CarController{
	
	// How many minimum units the wall is away from the player.
	//private int wallSensitivity = 2;
	
	
	private boolean isFollowingWall = false; // This is initialized when the car sticks to a wall.
	private WorldSpatial.RelativeDirection lastTurnDirection = null; // Shows the last turn direction the car takes.
	private boolean isTurningLeft = false;
	private boolean isTurningRight = false; 
	private WorldSpatial.Direction previousState = null; // Keeps track of the previous state
	
	private RouteAnalyser routeAnalyser;
	private HashMap<Coordinate, MapTile> map;
	private HashMap<Boolean,ArrayList<Coordinate>> seen;
	private HashMap<Boolean,ArrayList<Coordinate>> visited;
	private HashMap<Integer,Coordinate> keys;
	//private AIStrategy directionStratagy;
	//private Coordinate finishingPoint;
	private Coordinate lastUpdated;

	// Car Speed to move at
	private final float CAR_SPEED = 3;
	
	// Offset used to differentiate between 0 and 360 degrees
	private int EAST_THRESHOLD = 3;
	
	public MyAIController(Car car) {
		super(car);
		seen = new HashMap<>();
		visited = new HashMap<>();
		keys = new HashMap<>();
		routeAnalyser = new RouteAnalyser();
		Initialize();
	}
	
	/**
	 * Initialize the current map
	 */
	private void Initialize() {
		map = getMap();
		//HashMap contains two keys (true & false)
		//put every coordinate of the map to false
		//indicates they have not been seen or visited by the car
		
		seen.put(true, new ArrayList<Coordinate>());
		visited.put(true, new ArrayList<Coordinate>());
		seen.put(false, new ArrayList<Coordinate>());
		visited.put(false, new ArrayList<Coordinate>());
		
		for(Coordinate cord:map.keySet()) {
			seen.get(false).add(cord);
			visited.get(false).add(cord);

		}
	}
	
	/**
	 * update the map to reflect traps as the car moving
	 * @param current view of the car.
	 */
	private void updateCoordinates(HashMap<Coordinate, MapTile> currentView) {
		for(Coordinate cord:currentView.keySet()) {
			if(seen.get(false).contains(cord)) {
				seen.get(true).add(cord);
				seen.get(false).remove(cord);
			}
			if(map.containsKey(cord) && map.get(cord).getType() == MapTile.Type.ROAD ) {
				map.put(cord,currentView.get(cord));
			//	System.out.println(currentView.get(cord).getType());
			}
		}
	}
	
	//private Coordinate initialGuess;
	//boolean notSouth = true;
	@Override
	public void update(float delta) {
		
		// Gets what the car can see
		HashMap<Coordinate, MapTile> currentView = getView();

		Coordinate cord = null;
		updateCoordinates(currentView);
		
		
		if(lastUpdated == null) {
			lastUpdated = new Coordinate(getPosition());
			if(visited.get(false).contains(lastUpdated)) {
				visited.get(true).add(lastUpdated);
				visited.get(false).remove(lastUpdated);
			}
		}else {
			//current position of the car
			cord = new Coordinate(getPosition());
			
			//change strategy to driving following lava & wall
			//if the car is exiting a lava
			if(map.get(lastUpdated) instanceof LavaTrap && !(map.get(cord) instanceof LavaTrap)) {
				System.out.println("change stratagy");
				routeAnalyser.SwitchStrategy("WALL&LAVA");
			}
			
			// the car is going into a loop
			//change strategy to driving following wall
			if(!lastUpdated.equals(cord) && visited.get(true).contains(cord)){
				System.out.println();
				System.out.println("loop found");
				routeAnalyser.SwitchStrategy("WALL");
			
			// update the visited Hashmap as the car exploring
			}else if(!lastUpdated.equals(cord)){
				if(visited.get(false).contains(cord)) {
					System.out.println("updated");
					visited.get(true).add(cord);
					visited.get(false).remove(cord);
					lastUpdated = cord;
				}
			}
		}

		checkStateChange();

		// If you are not following a wall initially, find a wall to stick to!
		if (!isFollowingWall){
			//speed up
			if(getSpeed() < CAR_SPEED){
				applyForwardAcceleration();
			}
			// Turn towards the north
			if(!getOrientation().equals(WorldSpatial.Direction.NORTH)){
				lastTurnDirection = WorldSpatial.RelativeDirection.LEFT;
				System.out.println(getOrientation());
				applyLeftTurn(getOrientation(),delta);
			}
			if(routeAnalyser.ExploreMap(map, currentView, getOrientation(), new Coordinate(getPosition()))){
				// Turn right until we go back to east!
				if(!getOrientation().equals(WorldSpatial.Direction.EAST)){
					lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;
					System.out.println("east");
					System.out.println(getOrientation());
					applyRightTurn(getOrientation(),delta);
				}
				else{
					isFollowingWall = true;
				}
			}
		}
		// Once the car is already stuck to a wall, apply the following logic
		else{
			
			// Readjust the car if it is misaligned.
			readjust(lastTurnDirection,delta);
			
			if(isTurningRight){
				applyRightTurn(getOrientation(),delta);
			}
			else if(isTurningLeft){
				// Apply the left turn if you are not currently near a wall.
				if(!routeAnalyser.checkFollowingWall(getOrientation(),currentView,new Coordinate(getPosition()))){
					applyLeftTurn(getOrientation(),delta);
				}
				else{
					isTurningLeft = false;
				}
			}
			// Try to determine whether or not the car is next to a wall.
			else if(routeAnalyser.checkFollowingWall(getOrientation(),currentView,new Coordinate(getPosition()))){
				// Maintain some velocity
				if(getSpeed() < CAR_SPEED){
					applyForwardAcceleration();
				}
				// If there is wall ahead, turn right!
				if(routeAnalyser.checkWallAhead(getOrientation(),currentView,new Coordinate(getPosition()))){
//					applyReverseAcceleration();
//					readjust(lastTurnDirection,delta);
					lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;
					isTurningRight = true;	
//					if(routeAnalyser.checkWallAhead(getRightDirection(getOrientation()),currentView,new Coordinate(getPosition()))) {
//						applyReverseAcceleration();
//						readjust(lastTurnDirection,delta);
//					}else {
//					lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;
//					isTurningRight = true;	
//					}
					
				}

			}
			// This indicates that I can do a left turn if I am not turning right
			else{
				lastTurnDirection = WorldSpatial.RelativeDirection.LEFT;
				isTurningLeft = true;
			}
		}
		
		

	}
	
	/**
	 * Get the right direction based on our current orientation
	 * @param orientation
	 * @return
	 */
	private WorldSpatial.Direction getRightDirection(WorldSpatial.Direction orientation){
		switch(orientation){
		case EAST:
			return WorldSpatial.Direction.SOUTH;
		case NORTH:
			return WorldSpatial.Direction.EAST;
		case SOUTH:
			return WorldSpatial.Direction.WEST;
		case WEST:
			return WorldSpatial.Direction.NORTH;
			
		default:
			return null;
		}
	}
	
	/**
	 * Readjust the car to the orientation we are in.
	 * @param lastTurnDirection
	 * @param delta
	 */
	private void readjust(WorldSpatial.RelativeDirection lastTurnDirection, float delta) {
		if(lastTurnDirection != null){
			if(!isTurningRight && lastTurnDirection.equals(WorldSpatial.RelativeDirection.RIGHT)){
				adjustRight(getOrientation(),delta);
			}
			else if(!isTurningLeft && lastTurnDirection.equals(WorldSpatial.RelativeDirection.LEFT)){
				adjustLeft(getOrientation(),delta);
			}
		}
		
	}
	
	/**
	 * Try to orient myself to a degree that I was supposed to be at if I am
	 * misaligned.
	 */
	private void adjustLeft(WorldSpatial.Direction orientation, float delta) {
		
		switch(orientation){
		case EAST:
			if(getAngle() > WorldSpatial.EAST_DEGREE_MIN+EAST_THRESHOLD){
				turnRight(delta);
			}
			break;
		case NORTH:
			if(getAngle() > WorldSpatial.NORTH_DEGREE){
				turnRight(delta);
			}
			break;
		case SOUTH:
			if(getAngle() > WorldSpatial.SOUTH_DEGREE){
				turnRight(delta);
			}
			break;
		case WEST:
			if(getAngle() > WorldSpatial.WEST_DEGREE){
				turnRight(delta);
			}
			break;
			
		default:
			break;
		}
		
	}

	private void adjustRight(WorldSpatial.Direction orientation, float delta) {
		switch(orientation){
		case EAST:
			if(getAngle() > WorldSpatial.SOUTH_DEGREE && getAngle() < WorldSpatial.EAST_DEGREE_MAX){
				turnLeft(delta);
			}
			break;
		case NORTH:
			if(getAngle() < WorldSpatial.NORTH_DEGREE){
				turnLeft(delta);
			}
			break;
		case SOUTH:
			if(getAngle() < WorldSpatial.SOUTH_DEGREE){
				turnLeft(delta);
			}
			break;
		case WEST:
			if(getAngle() < WorldSpatial.WEST_DEGREE){
				turnLeft(delta);
			}
			break;
			
		default:
			break;
		}
		
	}
	
	/**
	 * Checks whether the car's state has changed or not, stops turning if it
	 *  already has.
	 */
	private void checkStateChange() {
		if(previousState == null){
			previousState = getOrientation();
		}
		else{
			if(previousState != getOrientation()){
				if(isTurningLeft){
					isTurningLeft = false;
				}
				if(isTurningRight){
					isTurningRight = false;
				}
				previousState = getOrientation();
			}
		}
	}
	
	/**
	 * Turn the car counter clock wise (think of a compass going counter clock-wise)
	 */
	private void applyLeftTurn(WorldSpatial.Direction orientation, float delta) {
		switch(orientation){
		case EAST:
			if(!getOrientation().equals(WorldSpatial.Direction.NORTH)){
				turnLeft(delta);
			}
			break;
		case NORTH:
			if(!getOrientation().equals(WorldSpatial.Direction.WEST)){
				turnLeft(delta);
			}
			break;
		case SOUTH:
			if(!getOrientation().equals(WorldSpatial.Direction.EAST)){
				turnLeft(delta);
			}
			break;
		case WEST:
			if(!getOrientation().equals(WorldSpatial.Direction.SOUTH)){
				turnLeft(delta);
			}
			break;
		default:
			break;
		
		}
		
	}
	
	/**
	 * Turn the car clock wise (think of a compass going clock-wise)
	 */
	private void applyRightTurn(WorldSpatial.Direction orientation, float delta) {
		switch(orientation){
		case EAST:
			if(!getOrientation().equals(WorldSpatial.Direction.SOUTH)){
				turnRight(delta);
			}
			break;
		case NORTH:
			if(!getOrientation().equals(WorldSpatial.Direction.EAST)){
				turnRight(delta);
			}
			break;
		case SOUTH:
			if(!getOrientation().equals(WorldSpatial.Direction.WEST)){
				turnRight(delta);
			}
			break;
		case WEST:
			if(!getOrientation().equals(WorldSpatial.Direction.NORTH)){
				turnRight(delta);
			}
			break;
		default:
			break;
		
		}
		
	}
//
//	/**
//	 * Check if you have a wall in front of you!
//	 * @param orientation the orientation we are in based on WorldSpatial
//	 * @param currentView what the car can currently see
//	 * @return
//	 */
//	private boolean checkWallAhead(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView){
//		switch(orientation){
//		case EAST:
//			return checkEast(currentView);
//		case NORTH:
//			return checkNorth(currentView);
//		case SOUTH:
//			return checkSouth(currentView);
//		case WEST:
//			return checkWest(currentView);
//		default:
//			return false;
//		
//		}
//	}
//	
//	/**
//	 * Check if the wall is on your left hand side given your orientation
//	 * @param orientation
//	 * @param currentView
//	 * @return
//	 */
//	private boolean checkFollowingWall(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView) {
//		
//		switch(orientation){
//		case EAST:
//			return checkNorth(currentView);
//		case NORTH:
//			return checkWest(currentView);
//		case SOUTH:
//			return checkEast(currentView);
//		case WEST:
//			return checkSouth(currentView);
//		default:
//			System.exit(1);
//			return false;
//		}
//		
//	}
//	
//
//	/**
//	 * Method below just iterates through the list and check in the correct coordinates.
//	 * i.e. Given your current position is 10,10
//	 * checkEast will check up to wallSensitivity amount of tiles to the right.
//	 * checkWest will check up to wallSensitivity amount of tiles to the left.
//	 * checkNorth will check up to wallSensitivity amount of tiles to the top.
//	 * checkSouth will check up to wallSensitivity amount of tiles below.
//	 */
//	public boolean checkEast(HashMap<Coordinate, MapTile> currentView){
//		// Check tiles to my right
//		Coordinate currentPosition = new Coordinate(getPosition());
//		for(int i = 0; i <= wallSensitivity; i++){
//			MapTile tile = currentView.get(new Coordinate(currentPosition.x+i, currentPosition.y));
//			if(directionStratagy.Driving(tile)){
//				return true;
//			}
//		}
//		return false;
//	}
//	
//	public boolean checkWest(HashMap<Coordinate,MapTile> currentView){
//		// Check tiles to my left
//		Coordinate currentPosition = new Coordinate(getPosition());
//		for(int i = 0; i <= wallSensitivity; i++){
//			MapTile tile = currentView.get(new Coordinate(currentPosition.x-i, currentPosition.y));
//			if(directionStratagy.Driving(tile)){
//				return true;
//			}
//		}
//		return false;
//	}
//	
//	public boolean checkNorth(HashMap<Coordinate,MapTile> currentView){
//		// Check tiles to towards the top
//		Coordinate currentPosition = new Coordinate(getPosition());
//		for(int i = 0; i <= wallSensitivity; i++){
//			MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y+i));
//			if(directionStratagy.Driving(tile)){
//				return true;
//			}
//		}
//		return false;
//	}
//	
//	public boolean checkSouth(HashMap<Coordinate,MapTile> currentView){
//		// Check tiles towards the bottom
//		Coordinate currentPosition = new Coordinate(getPosition());
//		for(int i = 0; i <= wallSensitivity; i++){
//			MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y-i));
//			if(directionStratagy.Driving(tile)){
//				return true;
//			}
//		}
//		return false;
//	}
//	
}
//
