import java.util.ArrayList;
import java.util.Random;
import java.util.HashMap;

import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

import tester.Tester;


class LightEmAll extends World {
  // a list of columns of GamePieces,
  // i.e., represents the board in column-major order
  ArrayList<ArrayList<GamePiece>> board;
  // a list of all nodes
  ArrayList<GamePiece> nodes;
  // a list of edges of the minimum spanning tree


  ArrayList<Edge> mst;
  // the width and height of the board 
  ArrayList<Edge> edges;
  // a list of edges of the minimum spanning tree


  int width;
  int height;
  int powerRadius;
  int powerstationRowIndex = 0;
  int powerstationColIndex = 0;
  Random random;
  int counter;

  LightEmAll(int width, int height, int radius) {
    this.width = width;
    this.height = height;
    this.random = new Random();
    this.board = this.buildGrid();
    this.nodes = new ArrayList<GamePiece>();
    this.powerRadius = this.findRadius();

    //    this.setConnectedNeighbors();
    //    this.setLighting();

    //WIP
    this.addCells();
    this.setIndices();
    this.edges = this.getPotentialEdges();
    new ArrayUtils().heapSort(edges);
    new ArrayUtils().reverse(edges);
    this.mst = this.kruszkalsGeneration();


    this.generateKruzalBoard();
    this.setConnectedNeighbors();
    this.powerRadius = this.findRadius();
    this.randomlyRotate();
    this.recalibrateLighting();
    this.setConnectedNeighbors();
    this.setLighting();


    //Leave alone
    powerstationRowIndex = this.getPowerstation().row;
    powerstationColIndex = this.getPowerstation().col;
  }

  // Constants
  public static int TILE_SIZE = 50;
  public static int PIXEL_WIDTH = 1000;
  public static int PIXEL_HEIGHT = 1000;

  //color constants
  public static WorldImage WIRE_COLOUR = 
      new RectangleImage(2, TILE_SIZE / 2, "solid", Color.GRAY);
  public static WorldImage LIT_WIRE = 
      new RectangleImage(2, TILE_SIZE / 2, "solid", Color.YELLOW);
  public static WorldImage POWERSTATION = 
      new StarImage(TILE_SIZE / 2, 5, OutlineMode.SOLID, Color.CYAN);

  // draws the 2 dimensional grid for the game
  public WorldScene makeScene() {
    WorldScene world = new WorldScene(PIXEL_WIDTH, PIXEL_HEIGHT);
    WorldImage winText = new TextImage("You Win!", 30, Color.GREEN);

    //set board true
    this.board.get(powerstationColIndex).get(powerstationRowIndex).powerStation = true;


    for (int i = 0; i < this.width; i++) {
      for (int n = 0; n < this.height; n++) {
        GamePiece currentpiece = this.board.get(i).get(n);
        //        if (this.withinRadius(currentpiece)) {
        //          currentpiece.lightUp();
        //        }
        world.placeImageXY(currentpiece.drawGamePiece(), i * TILE_SIZE + 60, n * TILE_SIZE + 60);

      }

    }

    if (this.itsLitYuh()) {
      world.placeImageXY(winText, 250, 250);
    }

    return world;
  }

  // creates indexes for all game pieces
  public ArrayList<ArrayList<GamePiece>> buildGrid() {

    ArrayList<ArrayList<GamePiece>> arrOfGamePieces = new ArrayList<ArrayList<GamePiece>>();
    this.assignPieces(arrOfGamePieces);
    this.addNodes(arrOfGamePieces);
    this.assignConnectorsFractal(arrOfGamePieces);
    return arrOfGamePieces;

  }

  // EFFECT: assigns game pieces to the board and assigns row & col for each game
  // piece
  public void assignPieces(ArrayList<ArrayList<GamePiece>> arr) {

    for (int i = 0; i < this.width; i++) {
      ArrayList<GamePiece> column = new ArrayList<GamePiece>();
      for (int n = 0; n < this.height; n++) {
        GamePiece g1 = new GamePiece();
        g1.col = i;
        g1.row = n;
        column.add(g1);

      }
      arr.add(column);
    }

    /// assigns neighbors for all pieces
    for (int i = 0; i < this.width; i++) {
      for (int j = 0; j < this.height; j++) {
        //GamePiece workingPiece = arr.get(i).get(j);

        //workingPiece.addNeighbors(arr, this.width, this.height);
      }
    }
  }


  public void onTick() {
    this.counter = this.counter + 1;
  }

  //                                                                   |
  // --------- | Kruzkal regeneration (and helper methods) | --------- v

  // Initializes LightEmAll Board
  // EFFECT: Adds empty GamePieces to the 2d ArrayList so it doesn't contain nulls
  void addCells() {
    for (int r = 0; r < this.height; r++) {
      this.board.add(new ArrayList<GamePiece>());
      for (int c = 0; c < this.width; c += 1) {
        this.board.get(r).add(new GamePiece(r, c));
      }
    }
  }

  // Gives each GamePiece in the board an index
  void setIndices() {
    int idx = 0;
    for (int r = 0; r < this.width; r++) {
      for (int c = 0; c < this.height; c++) {
        this.board.get(r).get(c).index = idx;
        idx++;
      }
    }
  }


  ArrayList<Edge> getPotentialEdges() {
    ArrayList<Edge> edges = new ArrayList<Edge>();
    for (int r = 0; r < this.width; r++) {
      for (int c = 0; c < this.height; c++) {
        GamePiece cur = this.board.get(r).get(c);
        // check for right edge
        if (c + 1 < this.width) {

          GamePiece next = this.board.get(r).get(c + 1);

          edges.add(new Edge(cur, next, this.random.nextInt()));

        }
        // check for left edge
        if (c - 1 >= 0) {
          GamePiece next = this.board.get(r).get(c - 1);

          edges.add(new Edge(cur, next, this.random.nextInt()));
        }
        // check for bottom edge
        if (r + 1 < this.width) {
          GamePiece next = this.board.get(r + 1).get(c);

          edges.add(new Edge(cur, next, this.random.nextInt()));
        }
        // check for top edge
        if (r - 1 >= 0) {
          GamePiece next = this.board.get(r - 1).get(c);

          edges.add(new Edge(cur, next, this.random.nextInt()));
        }
      }
    }
    return edges;
  }

  // Returns the minimum spanning tree (mst) of a list of heapsorted edges
  // (greatest to least OR descending order)
  ArrayList<Edge> kruszkalsGeneration() {
    // refers to each gamepiece's index
    HashMap<Integer, Integer> representatives = new HashMap<Integer, Integer>();
    ArrayList<Edge> mst = new ArrayList<Edge>();
    Queue<Edge> worklist = new Queue<Edge>();

    // Initializes node's representative to itself
    for (int r = 0; r < this.width; r++) {
      for (int c = 0; c < this.height; c++) {
        representatives.put(this.board.get(r).get(c).index, this.board.get(r).get(c).index);
      }
    }

    // Adds to worklist
    for (int i = 0; i < this.edges.size(); i++) {
      worklist.add(this.edges.get(i));
    }

    // int edges = 0;
    while (!worklist.isEmpty()) {
      Edge next = worklist.remove();

      int from = next.fromNode.index;
      int to = next.toNode.index;
      // Do they connect?
      if (this.getKey(representatives, from) != (this.getKey(representatives, to))) {
        mst.add(next);
        representatives.put(this.getKey(representatives, this.getKey(representatives, to)),
            this.getKey(representatives, from));
      }
    }
    return mst;
  }

  // Returns key of integer in HashMap
  int getKey(HashMap<Integer, Integer> representatives, int key) {
    if (representatives.get(key) == key) {

      return key;
    }
    else {
      return this.getKey(representatives, representatives.get(key));
    }
  }

  // Generates new random board with mst
  void generateKruzalBoard() {
    for (Edge e : this.mst) {
      GamePiece p1 = e.fromNode;
      GamePiece p2 = e.toNode;
      // same row
      if (p1.row == p2.row) {
        // right direction
        if (p1.col == p2.col - 1) {
          p1.right = true;
          p2.left = true;
        }
        else if (p1.col == p2.col + 1) {
          p1.left = true;
          p2.right = true;
        }
        // same column
      }
      else if (p1.col == p2.col) {
        if (p1.row == p2.row - 1) {
          p2.top = true;
          p1.bot = true;
        }
        else if (p1.row == p2.row + 1) {
          p2.bot = true;
          p1.top = true;
        }
      }
    }
    // places power station
    if (this.width > 0 && this.height > 0) {
      this.board.get(0).get(this.height / 2).powerStation = true;
    }
  }

  // Randomly rotates each game piece
  void randomlyRotate() {
    for (int w = 0; w < this.width; w += 1) {
      for (int h = 0; h < this.height; h += 1) {
        // random amount of times to rotate
        int n = this.random.nextInt(5) + 1;

        while (n < 6) {
          this.board.get(w).get(h).rotatePiece();
          n += 1;
        }
      }
    }
  }


  // --------- | Kruzkal regeneration (and helper methods) | --------- ^
  //                                                                   |


  // Fractal Board Generation
  void assignConnectorsFractal(ArrayList<ArrayList<GamePiece>> arr) {
    this.assignConnectorsFractalHelper(arr, 0, this.width - 1, 0, this.height - 1);

    // assign powerstation
    if (this.width > 0 && this.height > 0) {
      this.powerstationColIndex = this.height / 2;
      this.powerstationRowIndex = 0;
      //this.changePowerstationRow(this.height / 2);
    }
  }

  // Helper method to recur through arrayList
  void assignConnectorsFractalHelper(ArrayList<ArrayList<GamePiece>> arr, 
      int x1, int x2, int y1, int y2) {

    int middleX = (x2 + x1) / 2;
    int middleY = (y2 + y1) / 2;

    if (y2 == y1 || x2 == x1) {
      return; //reach end
    }

    // initial
    if ((x2 - x1 < 2 && y2 - y1 < 2)) {
      arr.get(x1).get(y1).bot = true;
      arr.get(x2).get(y1).bot = true;
      arr.get(x1).get(y2).top = true;
      arr.get(x1).get(y2).right = true;
      arr.get(x2).get(y2).top = true;
      arr.get(x2).get(y2).left = true;
    } else {

      //Direction to move: topLeft -> botLeft -> botRight -> topRight

      // Top left
      GamePiece topLeft = arr.get(x1).get(y1);
      topLeft.bot = true;

      for (int i = y1 + 1; i < y2; i += 1) {
        GamePiece nextLeft = arr.get(x1).get(i);
        nextLeft.top = true;
        nextLeft.bot = true;
      }
      // Bottome left
      GamePiece botLeft = arr.get(x1).get(y2);
      botLeft.top = true;
      botLeft.right = true;

      for (int i = x1 + 1; i < x2; i += 1) {
        GamePiece nextBot = arr.get(i).get(y2);
        nextBot.left = true;
        nextBot.right = true;
      }

      // Bottom right
      GamePiece botRight = arr.get(x2).get(y2);
      botRight.top = true;
      botRight.left = true;

      for (int i = y1 + 1; i < y2; i += 1) {
        GamePiece nextRight = arr.get(x2).get(i);
        nextRight.top = true;
        nextRight.bot = true;
      }

      // Top right
      GamePiece topRight = arr.get(x2).get(y1);
      topRight.bot = true;

      //recursion
      this.assignConnectorsFractalHelper(arr, middleX + 1, x2, y1, middleY); 
      this.assignConnectorsFractalHelper(arr, x1, middleX, y1, middleY); 
      this.assignConnectorsFractalHelper(arr, x1, middleX, middleY + 1, y2);
      this.assignConnectorsFractalHelper(arr, middleX + 1, x2, middleY + 1, y2); 
    }
  }

  //hardcodes same path
  public void assignConnectors(ArrayList<ArrayList<GamePiece>> arr, int left, 
      int top, int height, int width) {

    //fixed pattern, vertical lines
    //top row single, bottom row single
    //middle row/middle rows crossed
    //all rows between middle and top and bottom rows straight
    //beginning middle row and ending middle row prongs
    int middle = height / 2;
    for (int j = 0; j < height; j++) {
      for (int i = 0; i < width; i++) {
        if (j == 0) {
          //top
          arr.get(i).get(j).bot = true;
        } else if ((j > 0 && j < width - 1)) {
          //middle
          //middle row
          if (j == middle) {
            //first of middle row
            if (i == 0) {
              arr.get(i).get(j).top = true;
              arr.get(i).get(j).right = true;
              arr.get(i).get(j).bot = true;
            } else if (i == width - 1) {
              //last of middle row
              arr.get(i).get(j).top = true;
              arr.get(i).get(j).left = true;
              arr.get(i).get(j).bot = true;
            } else {
              //everything middle not those
              arr.get(i).get(j).top = true;
              arr.get(i).get(j).bot = true;
              arr.get(i).get(j).left = true;
              arr.get(i).get(j).right = true;
            } 
            //not middle row
          } else {
            arr.get(i).get(j).top = true;
            arr.get(i).get(j).bot = true;
          }
        } else if (j == width - 1) {
          //bottom
          arr.get(i).get(j).top = true;
        }
      }
    }
  }


  // EFFECT: updates the list of nodes to include all the pieces in the game board
  public void addNodes(ArrayList<ArrayList<GamePiece>> arr) {
    ArrayList<GamePiece> workingarr = new ArrayList<GamePiece>();
    for (int i = 0; i < this.width; i++) {
      for (int n = 0; n < this.height; n++) {
        GamePiece currentpiece = arr.get(i).get(n);
        workingarr.add(currentpiece);
      }
    }
    this.nodes = workingarr;
  }

  //finds radius of the board
  int findRadius() {
    GamePiece farthest = this.findFarthestPiece(this.getPowerstation());

    // (diameter) / 2 + 1
    return this.findDepth(farthest) / 2 + 1;
  }

  // Returns GamePiece where Powerstation is
  GamePiece getPowerstation() {
    GamePiece powerStation = new GamePiece();
    for (int i = 0; i < this.width; i++) {
      for (int j = 0; j < this.height; j++) {
        if (this.board.get(i).get(j).powerStation) {
          powerStation = this.board.get(i).get(j);
        }
      }
    }
    return powerStation;
  }

  // Returns the farthest piece from the PowerStation
  GamePiece findFarthestPiece(GamePiece start) {
    // worklist
    Queue<GamePiece> worklist = new Queue<GamePiece>();
    ArrayList<GamePiece> alreadySeen = new ArrayList<GamePiece>();

    // current GamePiece you're at
    GamePiece recent = start;

    // adds PowerStation to tail
    worklist.add(start);
    while (!worklist.isEmpty()) {
      recent = worklist.remove();
      for (GamePiece neighbor : recent.neighbors) {
        if (!alreadySeen.contains(neighbor)) {
          worklist.add(neighbor);
        }
      }
      alreadySeen.add(recent);
    }
    return recent;
  }

  // Returns depth from starting GamePiece to farthest possible GamePiece
  int findDepth(GamePiece start) {

    Queue<GamePiece> worklist = new Queue<GamePiece>();

    Queue<Integer> depth = new Queue<Integer>();
    ArrayList<GamePiece> alreadySeen = new ArrayList<GamePiece>();

    GamePiece recent = start;

    int dist = 0; //current depth from starting piece

    worklist.add(start); //add beginning piece to tail

    depth.add(0); // add beginning depth to tail

    while (!worklist.isEmpty()) {
      recent = worklist.remove();
      dist = depth.remove();
      for (GamePiece neighbor : recent.neighbors) {
        if (!alreadySeen.contains(neighbor)) {
          worklist.add(neighbor);
          depth.add(dist + 1);
        }
      }
      alreadySeen.add(recent);
      dist += 1;
    }
    return dist;
  }

  // Resets all the lighting
  // EFFECT: sets every GamePiece's lit field to false
  void recalibrateLighting() {
    for (int r = 0; r < board.size(); r += 1) {
      for (int c = 0; c < board.get(r).size(); c += 1) {
        this.board.get(r).get(c).connected = false;
        this.board.get(r).get(c).neighbors = new ArrayList<GamePiece>();
      }
    }
  }

  // Assigns each GamePiece a list of its *connected* neighbors
  void setConnectedNeighbors() {
    for (int w = 0; w < this.width; w += 1) {
      for (int h = 0; h < this.height; h += 1) {
        this.board.get(w).get(h).addNeighbors(this.board, this.width, this.height);
      }
    }
  }

  // Finds which pieces should be lighting, produces flood effect
  void setLighting() {
    this.powerRadius = this.findRadius();
    for (int w = 0; w < board.size(); w += 1) {
      for (int h = 0; h < board.get(w).size(); h += 1) {
        this.board.get(w).get(h).setLights(this.powerRadius);
      }
    }
  }

  // determines if the currentpiece is within the powerstation radius
  boolean withinRadius(GamePiece current) {

    int distance = (int) Math.sqrt(Math.abs(
        (current.col - this.powerstationColIndex) * (current.col - this.powerstationColIndex))
        + Math.abs((current.row - this.powerstationRowIndex) 
            * (current.row - this.powerstationRowIndex)));

    return distance < this.powerRadius;

  }

  // EFFECT: updates a list of edges with weights for the minimum spanning tree
  public void assignWeights(ArrayList<Edge> unweightededges) {
    for (Edge e : unweightededges) {
      int r = this.random.nextInt();
      e.weight = r;
    }
  }

  // EFFECT: rotates the piece that the user clicks on
  public void onMouseClicked(Posn pos, String buttonName) {
    int x = (pos.x - 40) / TILE_SIZE;
    int y = (pos.y - 40) / TILE_SIZE;

    //    for (int i = 0; i < this.width; i++) {
    //      for (int j = 0; j < this.height; j++) {
    //        GamePiece currentGamepiece = this.board.get(i).get(j);
    //        
    //        if (this.withinRadius(currentGamepiece)) {
    //          currentGamepiece.lightUp();
    //        }
    //      }   
    //    }

    if (validGamePiece(x, y)) {
      this.board.get(x).get(y).rotatePiece();
    }

    for (int i = 0; i < this.width; i++) {
      for (int j = 0; j < this.height; j++) {
        this.board.get(i).get(j).connected = false;
      }
    }

    this.recalibrateLighting();
    this.setConnectedNeighbors();
    this.setLighting();
    this.findRadius();
  }

  // Checks if user clicked in GamePiece
  boolean validGamePiece(int row, int col) {
    return row >= 0 && col >= 0 && row < this.width && col < this.height;
  }

  // checks to make sure that the powerstation is in bounds
  int changePowerstationColumn(int newPos) {

    if (this.powerstationColIndex + newPos >= 
        0 && this.powerstationColIndex + newPos < this.width) {

      return this.powerstationColIndex + newPos;
    }
    else {
      return this.powerstationColIndex;
    }
  }

  int changePowerstationRow(int newPos) {

    if (this.powerstationRowIndex + newPos >= 0 && 
        this.powerstationRowIndex + newPos < this.height) {
      return this.powerstationRowIndex + newPos;
    }

    else {
      return this.powerstationRowIndex;
    }

  }

  // Moves Powerstation
  public void onKeyEvent(String key) {

    GamePiece powerpiece = this.board.
        get(powerstationColIndex).get(powerstationRowIndex);
    GamePiece piecetotheleft = this.board
        .get(this.changePowerstationColumn(-1)).get(powerstationRowIndex);
    GamePiece piecetotheright = this.board
        .get(this.changePowerstationColumn(1)).get(powerstationRowIndex);
    GamePiece piecetothetop = this.board
        .get(powerstationColIndex).get(this.changePowerstationRow(-1));
    GamePiece piecetothebottom = this.board
        .get(powerstationColIndex).get(this.changePowerstationRow(1));

    // ensures the the powerstation can only travel on a wire path
    if (key.equals("left") && piecetotheleft.right && powerpiece.left) {
      this.board.get(powerstationColIndex).get(powerstationRowIndex).powerStation = false;
      this.board.get(powerstationColIndex).get(powerstationRowIndex).connected = false;
      this.powerstationColIndex = this.changePowerstationColumn(-1);
    }
    if (key.equals("right") && piecetotheright.left && powerpiece.right) {
      this.board.get(powerstationColIndex).get(powerstationRowIndex).powerStation = false;
      this.board.get(powerstationColIndex).get(powerstationRowIndex).connected = false;

      this.powerstationColIndex = this.changePowerstationColumn(1);
    }
    if (key.equals("up") && piecetothetop.bot && powerpiece.top) {
      this.board.get(powerstationColIndex).get(powerstationRowIndex).powerStation = false;
      this.board.get(powerstationColIndex).get(powerstationRowIndex).connected = false;

      this.powerstationRowIndex = this.changePowerstationRow(-1);
    }
    if (key.equals("down") && piecetothebottom.top && powerpiece.bot) {
      this.board.get(powerstationColIndex).get(powerstationRowIndex).powerStation = false;
      this.board.get(powerstationColIndex).get(powerstationRowIndex).connected = false;

      this.powerstationRowIndex = this.changePowerstationRow(1);

    }

  }

  public void onKeyReleased(String key) {
    System.out.println("Here");
    recalibrateLighting();
    setConnectedNeighbors();
    setLighting();
  }

  // determines if all wires are connected
  public boolean itsLitYuh() {
    for (int k = 0; k < this.width; k++) {

      for (int l = 0; l < this.height; l++) {

        if (!this.board.get(k).get(l).connected) {
          return false;
        }
      }
    }
    return true;

  }
}

class GamePiece {
  //in logical coordinates, with the origin
  //at the top-left corner of the screen
  int row;
  int col;
  // whether this GamePiece is connected to the
  // adjacent left, right, top, or bottom pieces
  boolean left;
  boolean right;
  boolean top;
  boolean bot;

  // whether the power station is on this piece
  boolean powerStation;

  // whether gamepiece has connection to powerstation
  boolean connected;

  //LoNeighbors
  ArrayList<GamePiece> neighbors;

  int index;

  GamePiece() {
    this.neighbors = new ArrayList<GamePiece>();
    index = 0;
  }

  // Actual Constructor used for Gameplay
  GamePiece(int row, int col) {
    this.row = row;
    this.col = col;
    this.index = 0;
    this.left = false;
    this.right = false;
    this.top = false;
    this.bot = false;
    this.powerStation = false;
    this.neighbors = new ArrayList<GamePiece>();
    this.connected = false;
  }

  public static WorldImage FLIPCONNECT = new RotateImage(LightEmAll.WIRE_COLOUR, 90.0);
  public static WorldImage FLIPCONNECTLIGHT = new RotateImage(LightEmAll.LIT_WIRE, 90.0);

  // draws GamePiece
  WorldImage drawGamePiece() {
    WorldImage mytile = new FrameImage(
        new RectangleImage(LightEmAll.TILE_SIZE, LightEmAll.TILE_SIZE, "solid", Color.DARK_GRAY));

    if (this.powerStation) {
      return new OverlayImage(LightEmAll.POWERSTATION,
          this.drawConnector(LightEmAll.LIT_WIRE, FLIPCONNECTLIGHT, mytile));
    }

    if (this.powerStation && !this.connected) {
      return new OverlayImage(LightEmAll.POWERSTATION,
          this.drawConnector(LightEmAll.WIRE_COLOUR, GamePiece.FLIPCONNECT, mytile));
    }

    if (this.connected) {
      return this.drawConnector(LightEmAll.LIT_WIRE, FLIPCONNECTLIGHT, mytile);
    }

    else {
      return this.drawConnector(LightEmAll.WIRE_COLOUR, FLIPCONNECT, mytile);
    }
  }

  // draws the connectors for this GamePiece
  WorldImage drawConnector(WorldImage originalConnect, WorldImage flipConnect, WorldImage base) {

    if (this.bot) {
      base = new OverlayOffsetImage(originalConnect, 0, -12.5, new FrameImage(
          new RectangleImage(LightEmAll.
              TILE_SIZE, LightEmAll.TILE_SIZE, "solid", Color.DARK_GRAY)));

    }
    if (this.top) {
      base = new OverlayOffsetImage(originalConnect, 0, 12.5, base);

    }

    if (this.left) {
      base = new OverlayOffsetImage(flipConnect, 12.5, 0, base);
    }

    if (this.right) {
      base = new OverlayOffsetImage(flipConnect, -12.5, 0, base);

    }

    return base;

  }

  void addNeighbors(ArrayList<ArrayList<GamePiece>> pieceList, int rows, int cols) {
    // checks for top neighbor and connection
    if (this.row - 1 >= 0) {
      if (this.top && pieceList.get(this.row - 1).get(this.col).bot) {
        this.neighbors.add(pieceList.get(this.row - 1).get(this.col));
      }
    }
    // left neighbor
    if (this.col - 1 >= 0) {
      if (this.left && pieceList.get(this.row).get(this.col - 1).right) {
        this.neighbors.add(pieceList.get(this.row).get(this.col - 1));
      }
    }
    // right neighbor
    if (this.col + 1 < cols - 1) {
      if (this.right && pieceList.get(this.row).get(this.col).left) {
        this.neighbors.add(pieceList.get(this.row).get(this.col + 1));
      }
    }
    // bottom neighbor
    if (this.row + 1 < rows - 1) {
      if (this.bot && pieceList.get(this.row).get(this.col).top) {
        this.neighbors.add(pieceList.get(this.row + 1).get(this.col));
      }
    }
  }

  //  //Assigns the neighbors of a tile
  //  public void assignNeighbors(ArrayList<ArrayList<GamePiece>> arr) {
  //    for (int i = 0; i < arr.size()-1; i++) {
  //      for (int n = 0; n < arr.get(i).size(); n++) {
  //       
  //        GamePiece currentPiece = arr.get(i).get(n);
  //       
  //        int totaldifference = Math.abs(this.col - currentPiece.col)
  //            + Math.abs(this.row - currentPiece.row);
  //
  //        if (totaldifference == 1) {
  //          this.neighbors.add(currentPiece);
  //        }
  //
  //      }
  //
  //    }
  //  } 

  //set to true if gamePiece contains powerstation, lights up all neighbors otherwise
  void setLights(int radius) {
    if (this.powerStation) {
      this.connected = true;
      this.lightUp(this, radius);
    }
  }

  // lights up the neighboring game pieces on the board
  void lightNeighbors(GamePiece powerStation, int radius) {
    for (int i = 0; i < this.neighbors.size(); i++) {
      if (!this.neighbors.get(i).connected
          && this.neighbors.get(i).findDistanceTo(powerStation) <= radius) {
        this.neighbors.get(i).connected = true;
        this.neighbors.get(i).lightNeighbors(powerStation, radius);
      }
    }
  }

  // Finds connected distance from this piece to the other
  int findDistanceTo(GamePiece to) {

    Queue<GamePiece> worklist = new Queue<GamePiece>();
    Queue<Integer> depth = new Queue<Integer>();
    ArrayList<GamePiece> alreadySeen = new ArrayList<GamePiece>();

    GamePiece recent = this;
    int dist = 0;

    worklist.add(this);
    depth.add(0);

    while (!worklist.isEmpty()) {
      recent = worklist.remove();
      dist = depth.remove();
      if (recent.equals(to)) {
        return dist;
      }
      for (GamePiece neighbor : recent.neighbors) {
        if (!alreadySeen.contains(neighbor)) {
          worklist.add(neighbor);
          depth.add(dist + 1);
        }
      }
      alreadySeen.add(recent);
      dist++;
    }
    return 999;
  }

  // EFFECT: lights up the cells that are connected the powerstation
  public void lightUp(GamePiece p, int radius) {

    if (this.powerStation) {
      this.connected = true;
    }

    if (this.connected & this.top) {

      for (GamePiece g : this.neighbors) {

        if (g.row == this.row - 1 && g.bot && !g.connected && g.findDistanceTo(p) <= radius) {
          g.connected = true;
          g.lightUp(p, radius);
        }
      }
    }

    if (this.connected & this.bot) {

      for (GamePiece g : this.neighbors) {

        if (g.row == this.row + 1 && g.top && !g.connected 
            && !g.connected && g.findDistanceTo(p) <= radius) {
          g.connected = true;
          g.lightUp(p, radius);
        }
      }
    }

    if (this.connected & this.right) {

      for (GamePiece g : this.neighbors) {

        if (g.col == this.col + 1 && g.left && !g.connected 
            && !g.connected && g.findDistanceTo(p) <= radius) {
          g.connected = true;
          g.lightUp(p, radius);
        }
      }
    }

    if (this.connected & this.left) {

      for (GamePiece g : this.neighbors) {

        if (g.col == this.col - 1 && g.right && !g.connected && 
            !g.connected && g.findDistanceTo(p) <= radius) {

          g.connected = true;
          g.lightUp(p, radius);
        }
      }
    }

  }

  //method to handle rotation of gamePiece on MouseClick
  void rotatePiece() {
    boolean lastLeft = this.left;
    boolean lastRight = this.right;
    boolean lastTop = this.top;
    boolean lastBot = this.bot;

    this.left = lastBot;
    this.top = lastLeft;
    this.right = lastTop;
    this.bot = lastRight;

  }

}

//Edges class to rep edges, for later
class Edge {

  GamePiece fromNode;
  GamePiece toNode;
  int weight;



  Edge(GamePiece fromNode, GamePiece toNode) {
    this.fromNode = fromNode;
    this.toNode = toNode;
    this.weight = 0;
  }

  // Secondary Constructor used for Testing Purposes
  Edge(GamePiece fromNode, GamePiece toNode, int weight) {
    this.fromNode = fromNode;
    this.toNode = toNode;
    this.weight = weight;
  }

  // Returns positive int if this weight is greater than other's
  // Returns negative int if this weight is less than other's
  // Returns 0 if this weight is the same as the other's
  public int compareWeight(Edge other) {
    if (this.weight > other.weight) {
      return 1;
    }
    else if (this.weight < other.weight) {
      return -1;
    }
    else {
      return 0;
    }
  }

  /*-
   * v, vertices
   * e, edges
   * :: v - 1 edges to connect
   */

}


class Queue<T> {

  Deque<T> contents;

  Queue() {
    this.contents = new Deque<T>();
  }

  public boolean isEmpty() {
    return this.contents.isEmpty();
  }

  public T remove() {
    return this.contents.removeFromHead();
  }

  public void add(T item) {
    this.contents.addAtTail(item);
  }
}

//Methods for ArrayLists of Edges
class ArrayUtils {

  // Heapsorts the list of edges
  // EFFECT: Modifies the given ArrayList
  public void heapSort(ArrayList<Edge> edges) {

    int length = edges.size();
    // Build heap
    for (int i = (length - 1) / 2; i >= 0; i -= 1) {
      this.downheap(edges, length, i);
    }

    for (int i = length - 1; i >= 0; i -= 1) {
      Edge temp = edges.get(0);
      edges.set(0, edges.get(i));
      edges.set(i, temp);

      this.downheap(edges, length, i);
    }
  }

  // Swaps two elements in an ArrayList
  // EFFECT: Modifies the given ArrayList
  public void swap(ArrayList<Edge> arr, int i, int j) {
    Edge temp = arr.get(i);
    arr.set(i, arr.get(j));
    arr.set(j, temp);
  }

  // Builds a heap
  // EFFECT: Modifies the given ArrayList
  public void downheap(ArrayList<Edge> arr, int size, int idx) {
    int leftIdx = 2 * idx + 1;
    int rightIdx = 2 * idx + 2;
    int biggestIdx = idx;

    // Is left bigger than original index?
    if (leftIdx < size && arr.get(leftIdx).weight > arr.get(idx).weight) {
      biggestIdx = leftIdx;
    }

    // Is right larger than the biggest?
    if (rightIdx < size && arr.get(rightIdx).weight > arr.get(biggestIdx).weight) {
      biggestIdx = rightIdx;
    }

    // If the biggest isn't original index
    if (biggestIdx != idx) {
      // Swap elements
      this.swap(arr, idx, biggestIdx);
      // Downheap recursively
      this.downheap(arr, size, biggestIdx);
    }
  }

  // Reverses an ArrayList
  // EFFECT: Modifies the given ArrayList
  public void reverse(ArrayList<Edge> arr) {
    for (int i = 0; i < arr.size() / 2; i += 1) {
      int idxLeft = i;
      int idxRight = arr.size() - 1 - i;

      Edge temp = arr.get(idxLeft);
      arr.set(idxLeft, arr.get(idxRight));
      arr.set(idxRight, temp);
    }
  }
}


//Represents a Deque (Double-ended Queue)
class Deque<T> {
  Sentinel<T> header; 

  // Sets Deque to a new Sentinel
  Deque() {
    this.header = new Sentinel<T>();
  }

  // Sets Deque to given Sentinel
  Deque(Sentinel<T> header) {
    this.header = header;
  }

  // Adds node to the front of the deque
  void addAtHead(T value) {
    this.header.addHead(value);
  }

  // Adds node to the back of the deque
  void addAtTail(T value) {
    this.header.addTail(value);
  }

  // Removes first node from deque
  T removeFromHead() {
    return this.header.removeHead();
  }

  // Removes last node from deque
  T removeFromTail() {
    return this.header.removeTail();
  }

  // Removes given node from the deque
  // If node not in deck, does nothing
  void removeNode(ANode<T> removeThis) {
    this.header.removeNodeHelp(removeThis, 0);
  }

  // Returns if deck is empty
  boolean isEmpty() {
    return this.header.isEmpty();
  }
}

//Represents Sentinel (Empty Node) or Node (Containing Data)
abstract class ANode<T> {
  ANode<T> next; 
  ANode<T> prev; 

  ANode(ANode<T> next, ANode<T> prev) {
    this.next = next;
    this.prev = prev;
  }

  // Returns the data of this node, null if sentinel
  abstract T getData();

  //remove node helper
  abstract void removeNodeHelp(ANode<T> removeThis, int looped);

  // Throws an exception if the given ANode is empty
  void exceptionIfEmpty() {
    if (this.next == this && this.prev == this) {
      throw new RuntimeException("Can't remove from an empty list.");
    }
  }
}

//Represents an abstract ANode containing no data
class Sentinel<T> extends ANode<T> {

  // Sentinel points to itself
  Sentinel() {
    super(null, null);

    this.next = this;
    this.prev = this;
  }

  // Adds a node to the head of a sentinel given its data value
  void addHead(T value) {
    this.next = new Node<T>(value, this.next, this);
  }

  // Adds a node to the tail of a sentinel given its data value
  void addTail(T value) {
    this.prev = new Node<T>(value, this, this.prev);
  }

  // Removes a node from the head of sentinel and returns the removed node's data
  T removeHead() {
    ANode<T> temp = this.next;
    this.exceptionIfEmpty();
    this.removeNodeHelp(this.next, 0);

    return temp.getData();
  }

  // Removes a node from the tail of sentinel and returns the removed node's data
  T removeTail() {
    ANode<T> temp = this.prev;
    this.exceptionIfEmpty();
    this.removeNodeHelp(this.prev, 0);

    return temp.getData();
  }

  // Retrieves the data represented by a sentinel
  T getData() {
    throw new RuntimeException("Can't pull data from null");
  }

  // Helper for removeNode 
  void removeNodeHelp(ANode<T> removeThis, int looped) {
    if (this.equals(removeThis) && looped >= 1) {
      return;
    }
    else {
      this.next.removeNodeHelp(removeThis, looped + 1);
    }
  }

  // Is the sentinel empty?
  boolean isEmpty() {
    return this == this.next;
  }
}

//Represents a node containing data
class Node<T> extends ANode<T> {
  T data;

  // Constructs a node with only a data value
  Node(T data) {
    super(null, null);
    this.data = data;
  }

  // Constructs a node with a data value, and the next and 
  // previous nodes it is linked to
  Node(T data, ANode<T> next, ANode<T> prev) {
    super(next, prev);
    this.data = data;

    if (next == null) {
      throw new IllegalArgumentException("The 'next' node is null");
    }

    if (prev == null) {
      throw new IllegalArgumentException("The 'prev' node is null");
    }

    prev.next = this;
    next.prev = this;
  }

  // Retrieves the data stored within this node
  T getData() {
    return this.data;
  }

  // Helper for removeNod
  void removeNodeHelp(ANode<T> removeThis, int looped) {
    if (this.equals(removeThis)) {
      this.prev.next = this.next;
      this.next.prev = this.prev;
    }
    else {
      this.next.removeNodeHelp(removeThis, looped);
    }
  }
}

class ExamplesLightEmAll {

  LightEmAll gameTest1;
  LightEmAll gameTest2;
  LightEmAll gameTest3;
  LightEmAll gameTest4;
  LightEmAll gameTest5;
  GamePiece gamePiece;
  GamePiece gamePiece1;


  void initData() {
    this.gameTest1 = new LightEmAll(8, 8, 10);
    this.gameTest2 = new LightEmAll(2, 2, 1);
    this.gameTest3 = new LightEmAll(8, 9, 10);
    this.gameTest4 = new LightEmAll(3, 4, 10);
    this.gameTest5 = new LightEmAll(6, 8, 10);

    this.gamePiece = new GamePiece();
    this.gamePiece1 = new GamePiece();

    gamePiece.left = true;
    gamePiece.top = true;

  }

  void testBigBang(Tester t) {
    initData();
    gameTest4.bigBang(700, 700, 1.0);

  }

  void testAssignPieces(Tester t) {
    initData();
    t.checkExpect(gameTest1.board.get(0).get(0).row, 0);
    t.checkExpect(gameTest1.board.get(1).get(3).row, 3);
    t.checkExpect(gameTest1.board.get(5).get(3).col, 5);
  }

  void testWithinRadius(Tester t) {
    initData();
    t.checkExpect(gameTest1.withinRadius(gameTest1.board.get(0).get(0)), true);
    t.checkExpect(gameTest4.withinRadius(gameTest4.board.get(7).get(7)), false);
  }

  void testallLitUp(Tester t) {
    initData();
    t.checkExpect(gameTest1.itsLitYuh(), false);
    t.checkExpect(gameTest3.itsLitYuh(), false);
  }

  void testAssignConnectors(Tester t) {
    initData();
    t.checkExpect(gameTest3.board.get(1).get(3).left, false);
    t.checkExpect(gameTest3.board.get(1).get(2).right, false);
    t.checkExpect(gameTest3.board.get(0).get(0).bot, true);
    gameTest1.height = 1;
    gameTest1.width = 1;
    t.checkExpect(gameTest1.board.get(0).get(0).bot, true);
    t.checkExpect(gameTest1.board.get(0).get(0).right, false);
    gameTest1.height = 2;
    gameTest1.width = 1;
    t.checkExpect(gameTest1.board.get(0).get(0).top, false);

  }

  void testRotatePiece(Tester t) {
    initData();
    gameTest3.board.get(0).get(0).rotatePiece();
    t.checkExpect(gameTest3.board.get(0).get(0).left, true);
    t.checkExpect(gameTest3.board.get(0).get(0).bot, false);
    t.checkExpect(gameTest3.board.get(0).get(0).right, false);
  }

  void testAssignNeigbors(Tester t) {
    initData();
    t.checkExpect(gameTest1.board.get(0).get(0).neighbors.size(), 4);
    t.checkExpect(gameTest1.board.get(3).get(3).neighbors.size(), 8);
    t.checkExpect(gameTest1.board.get(0).get(1).neighbors.size(), 6);

  }


  void testlightUp(Tester t) {
    initData();
    //gameTest3.board.get(3).get(3).lightUp();
    t.checkExpect(gameTest3.board.get(3).get(3).connected, false);
    t.checkExpect(gameTest1.board.get(5).get(5).connected, false);
    t.checkExpect(gameTest3.board.get(3).get(3).neighbors.get(0).connected, false);
  }


  void testResetLighting(Tester t) {
    initData();

    this.gameTest5.recalibrateLighting();

    t.checkExpect(this.gameTest5.board.get(2).get(3).connected, false);
    t.checkExpect(this.gameTest5.board.get(2).get(2).connected, false);
    t.checkExpect(this.gameTest5.board.get(2).get(4).connected, false);
    t.checkExpect(this.gameTest5.board.get(0).get(1).connected, false);
    t.checkExpect(this.gameTest5.board.get(1).get(3).connected, false);
  }

  // tests findNeighbors
  void testSetConnectedNeighbors(Tester t) {
    initData();

    this.gameTest5.board.get(2).get(1).right = true;
    this.gameTest5.board.get(2).get(2).left = true;

    this.gameTest5.setConnectedNeighbors();

    t.checkExpect(this.gameTest5.board.get(2).get(1).neighbors.size(), 12);
    t.checkExpect(this.gameTest5.board.get(2).get(2).neighbors.size(), 12);
    t.checkExpect(this.gameTest5.board.get(3).get(0).neighbors.size(), 9);
    t.checkExpect(this.gameTest5.board.get(2).get(2).neighbors.size(), 12);
  }


}