/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
 *
 * LibertyBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * LibertyBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */

package space.arim.libertybans.env.spigot.plugin;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import space.arim.libertybans.bootstrap.*;
import space.arim.libertybans.bootstrap.logger.BootstrapLogger;
import space.arim.libertybans.bootstrap.logger.JulBootstrapLogger;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public final class SpigotPlugin extends JavaPlugin {

	private BaseFoundation base;
	private ClassLoader launchLoader;

	@Override
	public synchronized void onEnable() {
		if (base != null) {
			throw new IllegalStateException("Plugin enabled twice?");
		}
		base = initialize();
	}

	@Override
	public synchronized void onDisable() {
		BaseFoundation base = this.base;
		this.base = null;
		ClassLoader launchLoader = this.launchLoader;
		this.launchLoader = null;

		if (base == null) {
			getLogger().warning("LibertyBans wasn't launched; check your log for a startup error");
		} else {
			base.shutdown();
		}
		// Close the dependency classloader so it - and everything it loaded, including the JDBC
		// driver and connection pool - can be garbage-collected. This matters when the plugin is
		// disabled and re-enabled without a full server restart, e.g. via PlugMan, which would
		// otherwise leak a classloader (and its loaded libraries) on every cycle.
		if (launchLoader instanceof Closeable closeable) {
			try {
				closeable.close();
			} catch (IOException ex) {
				getLogger().log(Level.WARNING, "Failed to close the LibertyBans classloader", ex);
			}
		}
	}

	static Platform.Builder detectPlatform(JavaPlugin plugin) {
		// Dynamically detect adventure based on whether it's implemented
		class AdventureLibraryDetection implements LibraryDetection {

			@Override
			public boolean evaluatePresence(BootstrapLogger logger) {
				try {
					Class<?> audienceClass = Class.forName("net.kyori.adventure.audience.Audience");
					return audienceClass.isAssignableFrom(Player.class);
				} catch (ClassNotFoundException ex) {
					return false;
				}
			}
		}
		Server server = plugin.getServer();
        ClassLoader platformClassLoader = JavaPlugin.class.getClassLoader();

		return Platform
				.builder(Platform.Category.BUKKIT)
				.nameAndVersion(server.getName(), server.getVersion())
				// On Spigot slf4j is an internal dependency
				.slf4jSupport(LibraryDetection.eitherOf(
						new LibraryDetection.Slf4jPluginLoggerMethod(plugin),
						new LibraryDetection.ByClassLoaderScan(ProtectedLibrary.SLF4J_API, platformClassLoader)
				))
				.kyoriAdventureSupport(new AdventureLibraryDetection())
                // Caffeine is an internal dependency on LeafMC, a Paper fork
                .caffeineProvided(new LibraryDetection.ByClassLoaderScan(ProtectedLibrary.CAFFEINE, platformClassLoader))
				.snakeYamlProvided(LibraryDetection.enabled());
	}

	private BaseFoundation initialize() {
		ExecutorService executor = Executors.newCachedThreadPool();

		Payload<JavaPlugin> payload;
		ClassLoader launchLoader;
		try {
			LibertyBansLauncher launcher = new LibertyBansLauncher.Builder()
					.folder(getDataFolder().toPath())
					.logger(new JulBootstrapLogger(getLogger()))
					.platform(detectPlatform(this))
					.executor(executor)
					.culpritFinder(new SpigotCulpritFinder())
					.build();
			payload = launcher.getPayload(this);
			launchLoader = launcher.attemptLaunch().join();
		} finally {
			executor.shutdown();
			assert executor.isTerminated();
		}
		// Retain the dependency classloader so it can be closed on disable
		this.launchLoader = launchLoader;
		BaseFoundation base;
		try {
			base = new Instantiator(
					"space.arim.libertybans.env.spigot.SpigotLauncher", launchLoader
			).invoke(payload);
		} catch (IllegalArgumentException | SecurityException | ReflectiveOperationException ex) {
			getLogger().log(Level.WARNING, "Failed to launch LibertyBans", ex);
			return null;
		}
		base.startup();
		return base;
	}

}
