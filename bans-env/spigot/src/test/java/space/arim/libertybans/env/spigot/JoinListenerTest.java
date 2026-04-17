/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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
package space.arim.libertybans.env.spigot;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.core.scope.ServerNameListener;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.libertybans.core.selector.cache.MuteCache;
import space.arim.libertybans.core.uuid.UUIDManager;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doReturn;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JoinListenerTest {

	private final FactoryOfTheFuture futuresFactory = new IndifferentFactoryOfTheFuture();

	@TempDir
	public Path tempDir;

	@Test
	public void registerInitialisesAlreadyOnlinePlayers(@Mock Guardian guardian,
														@Mock ServerNameListener<Player, ?> serverNameListener,
														@Mock SpigotEnforcer spigotEnforcer,
														@Mock UUIDManager uuidManager,
														@Mock MuteCache muteCache,
														@Mock Player player,
														@Mock PluginManager pluginManager) throws Exception {
		UUID uuid = UUID.randomUUID();
		InetAddress address = InetAddress.getByName("127.0.0.1");

		when(player.getUniqueId()).thenReturn(uuid);
		when(player.getName()).thenReturn("player");
		when(player.getAddress()).thenReturn(new InetSocketAddress(address, 25565));
		when(muteCache.cacheOnLogin(uuid, NetworkAddress.of(address))).thenReturn(futuresFactory.completedFuture(null));

		JavaPlugin plugin = MockJavaPlugin.create(tempDir, (Server server) -> {
			when(server.getPluginManager()).thenReturn(pluginManager);
			doReturn(List.of(player)).when(server).getOnlinePlayers();
		});

		JoinListener joinListener = new JoinListener(
				plugin, guardian, serverNameListener, spigotEnforcer, uuidManager, muteCache
		);
		joinListener.register();

		verify(serverNameListener).register();
		verify(uuidManager).addCache(uuid, "player");
		verify(muteCache).cacheOnLogin(eq(uuid), eq(NetworkAddress.of(address)));
		verify(guardian).onJoin(player, spigotEnforcer);
		verify(serverNameListener).onJoin(player, spigotEnforcer);
	}
}
