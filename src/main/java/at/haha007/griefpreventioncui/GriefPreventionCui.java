package at.haha007.griefpreventioncui;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class GriefPreventionCui extends JavaPlugin implements Listener {
	int distance;


	@Override
	public void onEnable() {
		saveDefaultConfig();
		reloadConfig();
		distance = getConfig().getInt("distance", 3);
		getServer().getPluginManager().registerEvents(this, this);
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::displayClaims, 10, 10);
	}

	private Collection<Claim> getNearbyClaims(Location center) {
		Collection<Claim> claims = new ArrayList<>();
		int radius = distance;
		Chunk lowest = center.subtract(radius, 0, radius).getChunk();
		Chunk highest = center.add(radius, 0, radius).getChunk();
		DataStore dataStore = GriefPrevention.instance.dataStore;
		for (int chunk_x = lowest.getX(); chunk_x <= highest.getX(); chunk_x++) {
			for (int chunk_z = lowest.getZ(); chunk_z <= highest.getZ(); chunk_z++) {
				claims.addAll(
					dataStore.
						getClaims(chunk_x, chunk_z).
						stream().
						filter(c -> c.inDataStore).
						collect(Collectors.toList()));
			}
		}
		return claims;
	}


	private void displayClaims() {
		Bukkit.getOnlinePlayers().stream().
			filter(player -> player.hasPermission("gpcui.display")).
			filter(player -> player.getInventory().getItemInMainHand().getType() == Material.GOLDEN_SHOVEL).
			forEach(player -> getNearbyClaims(player.getLocation()).
				spliterator().
				forEachRemaining(claim -> displayClaim(player, claim, false)));
	}


	private void displayClaim(Player player, Claim claim, boolean subclaim) {
		Location max = claim.getLesserBoundaryCorner();
		Location min = claim.getGreaterBoundaryCorner();
		int minX = Integer.min(max.getBlockX(), min.getBlockX());
		int minZ = Integer.min(max.getBlockZ(), min.getBlockZ());
		int maxX = Integer.max(max.getBlockX(), min.getBlockX()) + 1;
		int maxZ = Integer.max(max.getBlockZ(), min.getBlockZ()) + 1;

		Particle particle = subclaim ? Particle.SOUL_FIRE_FLAME : Particle.FLAME;
		displayZSurface(maxZ, player, minX, maxX, particle);
		displayZSurface(minZ, player, minX, maxX, particle);
		displayXSurface(maxX, player, minZ, maxZ, particle);
		displayXSurface(minX, player, minZ, maxZ, particle);
		//recursively execute for subclaims
		claim.children.stream().filter(c -> c.isNear(player.getLocation(), distance)).forEach(c -> displayClaim(player, c, true));
	}

	private void displayZSurface(int z, Player player, int minX, int maxX, Particle particle) {
		Location center = player.getLocation();
		int x = center.getBlockX();
		int y = center.getBlockY();
		center.setZ(z);
		int max = Math.min(maxX, x + distance);
		for (int px = Math.max(x - distance, minX); px <= max; px++) {
			if (px < minX || px > maxX) continue;
			for (int py = y - 2; py <= y + 1; py++) {
				player.spawnParticle(particle, new Location(center.getWorld(), px, py, z), 1, .2, 0, 0, 0);
			}
		}
	}

	private void displayXSurface(int x, Player player, int minZ, int maxZ, Particle particle) {
		Location center = player.getLocation();
		int z = center.getBlockZ();
		int y = center.getBlockY();
		center.setX(x);
		int max = Math.min(maxZ, z + distance);
		for (int pz = Math.max(z - distance, minZ); pz <= max; pz++) {
			for (int py = y - 2; py <= y + 1; py++) {
				player.spawnParticle(particle, new Location(center.getWorld(), x, py, pz), 1, 0, 0, .2, 0);
			}
		}
	}
}
