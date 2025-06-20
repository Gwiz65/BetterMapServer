/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 * 
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 * 
 * For more information, please refer to <http://unlicense.org/>
*/

package org.gwiz.wurmunlimited.mods;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.Versioned;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.gotti.wurmunlimited.mods.serverpacks.api.ServerPacks;
import org.gotti.wurmunlimited.mods.serverpacks.api.ServerPacks.ServerPackOptions;

import com.wurmonline.mesh.MeshIO;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.DbConnector;
import com.wurmonline.server.Server;
import com.wurmonline.server.Servers;

public class BetterMapServer implements WurmServerMod, ServerStartedListener, Configurable, Versioned {

	private static final String version = "1.0";
	private boolean allowDeeds = true;
	private boolean allowStartTowns = true;
	private boolean allowSoulfallStones = true;

	@Override
	public void configure(Properties properties) {
		allowDeeds = Boolean.parseBoolean(properties.getProperty("allowDeeds", "true"));
		allowStartTowns = Boolean.parseBoolean(properties.getProperty("allowStartTowns", "true"));
		allowSoulfallStones = Boolean.parseBoolean(properties.getProperty("allowSoulfallStones", "true"));
	}

	@Override
	public void onServerStarted() {
		byte[] data = null;
		String mapname = Servers.localServer.mapname.toLowerCase(Locale.ROOT).replace(" ", "_");
		String mapping = "bettermap_" + mapname + ".map = map/bettermap_" + mapname + ".png\nbettermap_" + mapname
				+ ".xml = xml/bettermap_" + mapname + ".xml";
		String xmlfiletxt = createXmlFileText();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(os)) {
			zos.putNextEntry(new ZipEntry("map/bettermap_" + mapname + ".png"));
			ImageIO.write(renderMapImage(Server.surfaceMesh), "PNG", zos);
			zos.closeEntry();
			zos.putNextEntry(new ZipEntry("xml/bettermap_" + mapname + ".xml"));
			zos.write(xmlfiletxt.getBytes(StandardCharsets.UTF_8));
			zos.closeEntry();
			zos.putNextEntry(new ZipEntry("mappings.txt"));
			zos.write(mapping.getBytes(StandardCharsets.UTF_8));
			zos.closeEntry();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		data = os.toByteArray();
		if (data != null) {
			ServerPacks.getInstance().addServerPack("bettermap_" + mapname, data, ServerPackOptions.FORCE,
					ServerPackOptions.PREPEND);
		}
	}

	private String createXmlFileText() {
		String outtext = "<markers>";
		try {
			Connection dbcon = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			dbcon = DbConnector.getZonesDbCon();
			ps = dbcon.prepareStatement("Select * from VILLAGES");
			rs = ps.executeQuery();
			while (rs.next()) {
				String name = rs.getString("NAME");
				int xpos = ((rs.getInt("STARTX") + rs.getInt("ENDX")) / 2);
				int ypos = ((rs.getInt("STARTY") + rs.getInt("ENDY")) / 2);
				boolean isStartTown = rs.getBoolean("PERMANENT");
				boolean isDisbanded = rs.getBoolean("DISBAND");
				if ((isStartTown && allowStartTowns && !isDisbanded) || (!isStartTown && allowDeeds && !isDisbanded)) {
					outtext = outtext + "<" + name.toLowerCase(Locale.ROOT).replace(" ", "_") + ">";
					outtext = outtext + "<name>" + name + "</name>";
					outtext = outtext + "<xpos>" + xpos + "</xpos>";
					outtext = outtext + "<ypos>" + ypos + "</ypos>";
					if (isStartTown)
						outtext = outtext + "<type>1</type>";
					else
						outtext = outtext + "<type>0</type>";
					outtext = outtext + "</" + name.toLowerCase(Locale.ROOT).replace(" ", "_") + ">";
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		if (allowSoulfallStones) {
			try {
				Connection dbcon = null;
				PreparedStatement ps = null;
				ResultSet rs = null;
				dbcon = DbConnector.getItemDbCon();
				ps = dbcon.prepareStatement("Select * from ITEMS WHERE TEMPLATEID=1016");
				rs = ps.executeQuery();
				while (rs.next()) {
					String name = rs.getString("DESCRIPTION");
					int xpos = (int) Math.round(rs.getFloat("POSX") / 4.0F);
					int ypos = (int) Math.round(rs.getFloat("POSY") / 4.0F);
					outtext = outtext + "<" + name.toLowerCase(Locale.ROOT).replace(" ", "_") + ">";
					outtext = outtext + "<name>" + name + "</name>";
					outtext = outtext + "<xpos>" + xpos + "</xpos>";
					outtext = outtext + "<ypos>" + ypos + "</ypos>";
					outtext = outtext + "<type>2</type>";
					outtext = outtext + "</" + name.toLowerCase(Locale.ROOT).replace(" ", "_") + ">";
				}
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		outtext = outtext + "</markers>";
		return outtext;
	}

	private BufferedImage renderMapImage(final MeshIO mesh) {
		int size = mesh.getSize();
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		int[] pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
		for (int x = 0; x < size - 1; x++) {
			for (int y = 0; y < size - 1; y++) {
				final int tileId = mesh.getTile(x, y);
				final byte type = Tiles.decodeType(tileId);
				final Tiles.Tile tile = Tiles.getTile(type);
				float h1 = Tiles.decodeHeight(tileId);
				float h2 = Tiles.decodeHeight(mesh.getTile(x, y + 1));
				float h3 = Tiles.decodeHeight(mesh.getTile(x + 1, y + 1));
				int color = shade(tile.getColor().getRGB(), h3 - h1, h1 <= 0);
				int start = (int) (y - (h1 / 50));
				int end = (int) (start + (Math.abs(h2 - h1) / 50) + 2);
				if (end >= size)
					end = size - 1;
				if (start < 0)
					start = 0;
				for (int i = start; i <= end; i++) {
					pixels[i * size + x] = color;
				}
			}
		}
		BufferedImage img2 = new BufferedImage(4096, 4096, BufferedImage.TYPE_INT_RGB);
		img2.createGraphics().drawImage(img, 0, 0, 4096, 4096, null);
		return img2;
	}

	private int shade(int color, float delta, boolean water) {
		int r = (color >> 16) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = color & 0xFF;
		float mult = (float) (1 + (Math.tanh(delta / 128) * 0.66));
		r *= mult;
		g *= mult;
		b *= mult;
		if (water) {
			r = (r / 5) + 41;
			g = (r / 5) + 51;
			b = (r / 5) + 102;
		}
		if (r < 0)
			r = 0;
		if (g < 0)
			g = 0;
		if (b < 0)
			b = 0;
		if (r > 255)
			r = 255;
		if (g > 255)
			g = 255;
		if (b > 255)
			b = 255;
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	@Override
	public String getVersion() {
		return version;
	}
}
