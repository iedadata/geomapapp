package haxby.db.mb;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;

import javax.imageio.ImageIO;

import org.geomapapp.geom.MapProjection;
import org.geomapapp.grid.Grid2D;
import org.geomapapp.grid.Grid2DOverlay;
import org.geomapapp.grid.GridDialog;
import org.geomapapp.grid.TileIO;
import org.geomapapp.grid.TiledGrid;

import haxby.map.FocusOverlay;
import haxby.map.MapApp;
import haxby.map.MapOverlay;
import haxby.map.Overlay;
import haxby.map.Tile;
import haxby.map.XMap;
import haxby.proj.PolarStereo;
import haxby.proj.Projection;
import haxby.proj.ProjectionFactory;
import haxby.util.URLFactory;

public class PreviewCruise
{
  private static int MAP_PROJ;
  
  public static void main(String[] inputArgs)
  {
    if ((inputArgs.length != 1) && (inputArgs.length != 2))
    {
      System.err.println("Usage: PreviewCruise cruiseDir [maxRes]");
      System.exit(-1);
    }
    while (inputArgs[0].endsWith("/")) {
      inputArgs[0] = inputArgs[0].substring(0, inputArgs[0].length() - 1);
    }
    int maxRes = inputArgs.length == 2 ? Integer.parseInt(inputArgs[1]) : 512;
    String cruiseID = inputArgs[0].substring(inputArgs[0].lastIndexOf("/") + 1);
    String cruiseDir = inputArgs[0];
    if ((!cruiseDir.startsWith("http://")) && (!cruiseDir.startsWith("file:/"))) {
      cruiseDir = "file://" + new File(cruiseDir).getPath();
    }
    System.setProperty("geomapapp.menus_url", "http://new.geomapapp.org/gma_menus/simple_menu.xml");
    MapApp mapApp = MapApp.createMapApp(new String[0]);
    XMap map = mapApp.getMap();
    final MBTracks tracks = new MBTracks(map, 4000, cruiseDir + "/mb_control");
    mapApp.addProcessingTask(tracks.getDBName(), new Runnable()
    {
      public void run()
      {
        mapApp.loadDatabase(tracks, null);
      }
    });
    MAP_PROJ = mapApp.getMapType();
    if (MAP_PROJ == 0) {
      cruiseDir = cruiseDir + "/merc/";
    } else if (MAP_PROJ == 1) {
      cruiseDir = cruiseDir + "/SP/";
    } else {
      cruiseDir = cruiseDir + "/NP/";
    }
    CruiseBounds cruiseBounds = new CruiseBounds(cruiseDir, map);
    map.addOverlay(cruiseID + " bounds", cruiseBounds);
    CruiseImageViewer cruiseImageViewer = new CruiseImageViewer(map, cruiseDir, maxRes);
    mapApp.addFocusOverlay(cruiseImageViewer, cruiseID + " images");
    String gridName = cruiseID + " grid";
    CruiseGridViewer cruiseGridViewer = new CruiseGridViewer(map, gridName, cruiseDir, maxRes);
    GridDialog.GRID_LOADERS.put(gridName, cruiseGridViewer);
    GridDialog.GRID_UNITS.put(gridName, "m");
    GridDialog.GRID_URL.put(gridName, cruiseDir);
    GridDialog gridDialog = mapApp.getMapTools().getGridDialog();
    gridDialog.gridCmds.put(gridName + "Cmd", gridName);
    gridDialog.addGrid(cruiseGridViewer);
    gridDialog.setSelectedGrid(cruiseGridViewer);
    gridDialog.showDialog();
    gridDialog.startGridLoad();
    gridDialog.loaded = true;

    int dx;
    int dy;
    if (mapApp.getMapType() == 0)
    {
      dx = 0;
      dy = 260;
    }
    else
    {
      dx = dy = 320;
    }
    cruiseBounds.dx = (cruiseGridViewer.dx = cruiseImageViewer.dx = dx);
    cruiseBounds.dy = (cruiseGridViewer.dy = cruiseImageViewer.dy = dy);
  }
  
  public static class CruiseBounds
    implements Overlay
  {
    private XMap map;
    private String base;
    private Rectangle2D.Double bounds;
    public int dx = 0;
    public int dy = 260;
    
    public CruiseBounds(String base, XMap map)
    {
      this.base = base;
      this.map = map;
      loadBounds();
    }
    
    private void loadBounds()
    {
      try
      {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL(this.base + "/bounds").openStream()));
        String str = bufferedReader.readLine();
        String[] values = str.split("\t");
        int xMin = Integer.parseInt(values[0]);
        int xMax = Integer.parseInt(values[1]);
        int yMin = Integer.parseInt(values[2]);
        int yMax = Integer.parseInt(values[3]);
        while (xMax - xMin > 1024) {
          xMax -= 1024;
        }
        this.bounds = new Rectangle2D.Double();
        this.bounds.x = xMin;
        this.bounds.y = yMin;
        this.bounds.width = (xMax - xMin + 1);
        this.bounds.height = (yMax - yMin + 1);
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
    }
    
    public void draw(Graphics2D graphics2D)
    {
      if (this.bounds == null) {
        return;
      }
      Rectangle area = graphics2D.getClipBounds();
      double wrap = this.map.getWrap();
      double scale = 0.625D;
      Rectangle2D.Double cruiseBounds = new Rectangle2D.Double(this.bounds.x * scale + this.dx, this.bounds.y * scale + this.dy, this.bounds.width * scale, this.bounds.height * scale);
      graphics2D.setStroke(new BasicStroke(2.0F / (float)this.map.getZoom()));
      graphics2D.setColor(Color.red);
      graphics2D.draw(cruiseBounds);
      if (wrap > 0.0D) {
        while (cruiseBounds.x < area.x + area.width)
        {
          cruiseBounds.x += wrap;
          graphics2D.draw(cruiseBounds);
        }
      }
    }
  }
  
  public static class CruiseGridViewer
    extends Grid2DOverlay
    implements GridDialog.GridLoader
  {
    private String base;
    private int maxRes;
    public int dx = 0;
    public int dy = 260;
    
    public CruiseGridViewer(XMap map, String name, String base, int maxRes)
    {
      super(map, name);
      this.base = base;
      this.maxRes = maxRes;
    }
    
    public void loadGrid(Grid2DOverlay grid)
    {
      XMap map = getXMap();
      double zoom = map.getZoom();
      int res = 1;
      while (zoom > res) {
        res *= 2;
      }
      if (res < 512) {
        return;
      }
      if (res < this.maxRes) {
        res = 512;
      }
      res = Math.min(this.maxRes, res);
      Rectangle2D rect = map.getClipRect2D();
      int x = (int)Math.floor(res * (rect.getX() - this.dx));
      int y = (int)Math.floor(res * (rect.getY() - this.dy));
      int width = (int)Math.ceil(res * (rect.getX() - this.dx + rect.getWidth())) - x;
      int height = (int)Math.ceil(res * (rect.getY() - this.dy + rect.getHeight())) - y;
      if ((width <= 0) || (height <= 0)) {
        return;
      }
      Projection proj = null;
      if (PreviewCruise.MAP_PROJ == 0) {
        proj = ProjectionFactory.getMercator(640 * res);
      } else if (PreviewCruise.MAP_PROJ == 1) {
        proj = new PolarStereo(new Point(0, 0), 180.0D, 25600.0D / res, -71.0D, 2, 2);
      } else {
        proj = new PolarStereo(new Point(0, 0), 0.0D, 25600.0D / res, 71.0D, 1, 2);
      }
      Rectangle bounds = new Rectangle(x, y, width, height);
      if ((bounds.width <= 0) || (bounds.height <= 0)) {
        return;
      }

      int iRes = res;
      int nLevel = 0;
      for (nLevel = 0; iRes >= 8; nLevel++) {
        iRes /= 8;
      }
      Grid2D.FloatWT grd = new Grid2D.FloatWT(bounds, (MapProjection)proj);
      TileIO.FloatWT tile = new TileIO.FloatWT((MapProjection)proj, this.base + "/zw_" + res, 320, nLevel);
      Rectangle rect2 = new Rectangle(-this.dx * res, -this.dy * res, 640 * res, this.dy * 2 * res);

      TiledGrid tiler = new TiledGrid((MapProjection)proj, rect2, tile, 320, 1, null);
      if (PreviewCruise.MAP_PROJ == 0) {
        tiler.setWrap(640 * res);
      }
      System.out.println("Compose Cruise Grid: " + res);
      grd = (Grid2D.FloatWT)tiler.composeGrid(grd);
	  
      Grid2D.Boolean land = new Grid2D.Boolean(bounds, proj);
      boolean hasLand = false;
      for (x = bounds.x; x < bounds.x + bounds.width; x++) {
        for (y = bounds.y; y < bounds.y + bounds.height; y++)
        {
          boolean tf = grd.floatValue(x, y) >= 0.0F;
          land.setValue(x, y, tf);
          if (tf) {
            hasLand = true;
          }
        }
      }
      setGrid(grd, land, hasLand, true);
    }
  }
  
  public static class CruiseImageViewer
    extends MapOverlay
    implements FocusOverlay
  {
    public int dx = 0;
    public int dy = 260;
    private String base;
    private int CACHE_SIZE = 20;
    private Vector<Tile> tiles = new Vector<Tile>(this.CACHE_SIZE);
    private int maxRes;
    
    public CruiseImageViewer(XMap map, String base, int maxRes)
    {
      super(map);
      this.base = base;
      this.maxRes = maxRes;
    }
    
    public Runnable createFocusTask(final Rectangle2D rect)
    {
      return new Runnable()
      {
        public void run()
        {
          PreviewCruise.CruiseImageViewer.this.focus(rect);
          PreviewCruise.CruiseImageViewer.this.getMap().repaint();
        }
      };
    }
    
    public void focus(Rectangle2D rect)
    {
      getImage(rect, getMap().getZoom());
    }
    
    
	private boolean getImage(Rectangle2D rect, double zoom)
    {
      int res = 1;
      while (zoom > res) {
        res *= 2;
      }
      if (res < 512) {
        return false;
      }
      if (res < this.maxRes) {
        res = 512;
      }
      res = Math.min(res, this.maxRes);
      int scale = res;
      int x = (int)Math.floor(scale * (rect.getX() - this.dx));
      int y = (int)Math.floor(scale * (rect.getY() - this.dy));
      int width = (int)Math.ceil(scale * (rect.getX() - this.dx + rect.getWidth())) - x;
      int height = (int)Math.ceil(scale * (rect.getY() - this.dy + rect.getHeight())) - y;
      
      if ((width <= 0) || (height <= 0)) {
        return false;
      }
      
      BufferedImage image = new BufferedImage(width, height, 2);
      Graphics2D graphics = image.createGraphics();
      graphics.setColor(Color.gray);
      graphics.setComposite(AlphaComposite.Clear);
      graphics.fillRect(0, 0, width, height);
      
      BufferedImage mapImage = null;
      Rectangle mapRect = new Rectangle();
      if (res == getResolution()) {
        mapRect = getRect();
        if (mapRect.contains(x, y, width, height)) {
          return false;
        }
        mapImage = getImage();
      }
      
      int tileX0 = x / 320;
      if ((x < 0) && (tileX0 * 320 != x)) {
        tileX0--;
      }
      int tileY0 = y / 320;
      if ((y < 0) && (tileY0 * 320 != y)) {
        tileY0--;
      }
	  int tileX, tileY;
	  int x0,y0;
	  int x1,x2,y1,y2;
      
      for (tileX = tileX0; tileX * 320 < x + width; tileX++) {
        x0 = tileX * 320;
        x1 = Math.max(x0, x);
        x2 = Math.min(x0 + 329, x + width); //***
        for (tileY = tileY0; tileY * 320 < y + height; tileY++) {
          y0 = tileY * 320;
          y1 = Math.max(y0, y);
          y2 = Math.min(y0 + 320, y + height);
          if ((mapImage != null) && (mapRect.contains(x1 + this.dx * scale, y1 + this.dy * scale, x2 - x1, y2 - y1))) {
            for (int ix = x1; ix < x2; ix++) {
              for (int iy = y1; iy < y2; iy++) {
                image.setRGB(ix - x, iy - y, mapImage.getRGB(ix - mapRect.x + this.dx * scale, iy - mapRect.y + this.dy * scale));
              }
            }
            continue;
          }

            BufferedImage tile;
            try
            {
              tile = getTile(res, tileX, tileY);
              if (tile == null) {
                continue;
              }
            }
            catch (Exception ex)
            {
              continue;
            }
            for (int ix = x1; ix < x2; ix++) {
              for (int iy = y1; iy < y2; iy++)
              {
                int tX = ix - x0;
                int tY = iy - y0;
                if ((tX >= 0) && (tY >= 0) && (tX < tile.getWidth()) && (tY < tile.getWidth())) {
                  image.setRGB(ix - x, iy - y, tile.getRGB(ix - x0, iy - y0));//***
                }
              }
            }
          }
        
      }
      x += this.dx * scale;
      y += this.dy * scale;
 
      setImage(image, x / (double) scale, y / (double) scale,
			1. / scale);
	  setRect(x, y, width, height);

      setResolution(res);
      return true;
    }
    
    public BufferedImage getTile(int res, int x, int y)
      throws IOException
    {
      int MAX_TRIES = 1;
      int wrap = res * 2;
      if (PreviewCruise.MAP_PROJ == 0)
      {
        while (x < 0) {
          x += wrap;
        }
        while (x >= wrap) {
          x -= wrap;
        }
      }
      Tile tile;
      for (int i = 0; i < this.tiles.size(); i++)
      {
        tile = (Tile)this.tiles.get(i);
        if ((res == tile.res) && (x == tile.x) && (y == tile.y))
        {
          if (i != 0) {
            synchronized (this.tiles)
            {
              this.tiles.remove(tile);
              this.tiles.add(0, tile);
            }
          }
          return ImageIO.read(new ByteArrayInputStream(tile.jpeg));
        }
      }
      int nGrid = res;
      int nLevel = 0;
      while (nGrid >= 8)
      {
        nLevel++;
        nGrid /= 8;
      }
      int factor = 8;
      for (int k = 1; k < nLevel; k++) {
        factor *= 8;
      }
      String name = "i_" + res;
      for (int k = 0; k < nLevel; k++)
      {
        int xG = factor * (int)Math.floor((double) x / (double)factor);
        int yG = factor * (int)Math.floor((double) y / (double)factor);
        name = name + "/" + getName(xG, yG);
        factor /= 8;
      }
      name = name + "/" + getName(x, y) + ".jpg";
      URL url = URLFactory.url(this.base + name);
      System.out.println(url);
      int tries = MAX_TRIES;
      while (true) {
    	  try {
    		  URLConnection con = url.openConnection();
    		  InputStream in = con.getInputStream();
    		  tile = new Tile(res, x, y, in, 0);
    		  break;
    	  }
    	  catch (BindException ex) {
    		  try {
    			  Thread.sleep(1000);
    		  } catch (InterruptedException e) {
    		  }
    	  }
    	  catch (IOException ex) {
    		  tries--;
    		  if (tries <= 0) {
    			  throw ex;
    		  }

    		  try {
    			  Thread.sleep(1000);
    		  } catch (InterruptedException e) {
    		  }
    	  }
      }
      synchronized (tiles) {
    	  if(tiles.size() == 0) {
    		  tiles.add(tile);
    	  } else if(tiles.size() == CACHE_SIZE) {
    		  tiles.remove(CACHE_SIZE - 1);
    		  tiles.add(0,tile);
    	  } else {
    		  tiles.add(0,tile);
    	  }
      }
      return ImageIO.read(new ByteArrayInputStream(tile.jpeg));
    }
    
    public String getName(int x0, int y0)
    {
      return (x0 >= 0 ? "E" + x0 : new StringBuilder().append("W").append(-x0).toString()) + (y0 >= 0 ? "N" + y0 : new StringBuilder().append("S").append(-y0).toString()) + "_320";
    }
  }
}
