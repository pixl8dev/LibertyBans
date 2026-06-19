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

package space.arim.libertybans.core.database;

import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.database.PunishmentDatabase;
import space.arim.libertybans.bootstrap.plugin.PluginInfo;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.database.execute.SQLRunnable;
import space.arim.libertybans.core.database.execute.SQLTransactionalFunction;
import space.arim.libertybans.core.database.execute.SQLTransactionalRunnable;
import space.arim.libertybans.core.database.sql.TableForType;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.DelayCalculators;
import space.arim.omnibus.util.concurrent.EnhancedExecutor;
import space.arim.omnibus.util.concurrent.ScheduledTask;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static space.arim.libertybans.core.schema.Tables.PUNISHMENTS;

public final class StandardDatabase implements InternalDatabase, AutoCloseable {

	private final Path folder;
	private final DatabaseManager manager;
	private final Vendor vendor;
	private final HikariDataSource dataSource;
	private final QueryExecutor queryExecutor;
	private final ExecutorService threadPool;
	private final PunishmentDatabase external = new External();

	private ScheduledTask expirationRefreshTask;
	private ScheduledTask synchronizationPollTask;
	private ScheduledTask localAddressCleanupTask;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	StandardDatabase(Path folder, DatabaseManager manager, Vendor vendor,
                     HikariDataSource dataSource, QueryExecutor queryExecutor, ExecutorService threadPool) {
        this.folder = folder;
        this.manager = manager;
		this.vendor = vendor;
		this.dataSource = dataSource;
		this.queryExecutor = queryExecutor;
		this.threadPool = threadPool;
	}

	/*
	 * Lifecycle
	 * 
	 * Guarded by the global lock on BaseFoundation lifecycle events
	 */

	void startTasks(Time time) {
		EnhancedExecutor enhancedExecutor = manager.enhancedExecutor();
		expirationRefreshTask = enhancedExecutor.scheduleRepeating(
				new RefreshTaskRunnable(manager, this, time),
				Duration.ofHours(3L),
				DelayCalculators.fixedDelay()
		);
		localAddressCleanupTask = enhancedExecutor.scheduleRepeating(
				() -> manager.deleteLocalAddresses().exceptionally((ex) -> {
					logger.warn("Failed to clean up local addresses from address history", ex);
					return null;
				}),
				Duration.ofMinutes(30L),
				DelayCalculators.fixedDelay()
		);
		var synchronizationConf = manager.configs().getSqlConfig().synchronization();
		if (synchronizationConf.enabled()) {
			synchronizationPollTask = enhancedExecutor.scheduleRepeating(
					manager.globalEnforcement(),
					Duration.ofMillis(synchronizationConf.pollRateMillis()),
					DelayCalculators.fixedDelay()
			);
		}
	}

	void cancelTasks() {
		expirationRefreshTask.cancel();
		localAddressCleanupTask.cancel();
		if (synchronizationPollTask != null) {
			synchronizationPollTask.cancel();
			synchronizationPollTask = null;
		}
	}

	@Override
	public void close() {
		threadPool.shutdown();
		try {
			boolean terminated = threadPool.awaitTermination(5L, TimeUnit.SECONDS);
			if (!terminated) {
				logger.warn("Reached timeout while waiting for thread pool");
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			logger.warn("Interrupted while waiting for thread pool", ex);
		}
		dataSource.close();
	}

	void closeCompletely() {
		if (getVendor() == Vendor.HSQLDB) {
			String command;
			if (checkResetLocalDatabaseCompact()) {
				command = "SHUTDOWN COMPACT";
				logger.info("Compacting the local database with {}", command);
			} else {
				command = "SHUTDOWN";
			}
			execute((context) -> context.query(command).execute()).join();
		}
		close();
		deregisterJdbcDrivers();
	}

	/*
	 * JDBC drivers register themselves with the global java.sql.DriverManager when their class is
	 * loaded. DriverManager then holds a strong reference to the driver, and through it, to the
	 * classloader that loaded the driver - which, for LibertyBans, is our dependency classloader.
	 * Left registered, this pins the classloader in memory so it cannot be garbage-collected when
	 * the plugin is disabled or reloaded (e.g. via PlugMan), leaking a classloader on every reload.
	 *
	 * We therefore deregister any driver loaded by our own classloader. This runs only from
	 * closeCompletely(), i.e. on full shutdown or a vendor-changing restart, never on a same-vendor
	 * restart (which reuses close()), so an in-use driver is never deregistered out from under us.
	 */
	private void deregisterJdbcDrivers() {
		ClassLoader ourLoader = getClass().getClassLoader();
		for (Driver driver : Collections.list(DriverManager.getDrivers())) {
			if (loadedBy(driver.getClass().getClassLoader(), ourLoader)) {
				try {
					DriverManager.deregisterDriver(driver);
					logger.debug("Deregistered JDBC driver {}", driver.getClass().getName());
				} catch (SQLException ex) {
					logger.warn("Unable to deregister JDBC driver {}", driver.getClass().getName(), ex);
				}
			}
		}
	}

	private static boolean loadedBy(ClassLoader candidate, ClassLoader ancestor) {
		for (ClassLoader loader = candidate; loader != null; loader = loader.getParent()) {
			if (loader == ancestor) {
				return true;
			}
		}
		return false;
	}

	@Override
	public PunishmentDatabase asExternal() {
		return external;
	}
	
	@Override
	public Vendor getVendor() {
		return vendor;
	}

	@Override
	public void executeWithExistingConnection(Connection connection, SQLTransactionalRunnable command) throws SQLException {
		queryExecutor.executeWithExistingConnection(connection, command);
	}

	@Override
	public <R> R queryWithExistingConnection(Connection connection, SQLTransactionalFunction<R> command) throws SQLException {
		return queryExecutor.queryWithExistingConnection(connection, command);
	}

	@Override
	public CentralisedFuture<Void> execute(SQLRunnable command) {
		return queryExecutor.execute(command);
	}

	@Override
	public <R> CentralisedFuture<R> query(SQLFunction<R> command) {
		return queryExecutor.query(command);
	}

	@Override
	public CentralisedFuture<Void> executeWithRetry(int retryCount, SQLTransactionalRunnable command) {
		return queryExecutor.executeWithRetry(retryCount, command);
	}

	@Override
	public <R> CentralisedFuture<R> queryWithRetry(int retryCount, SQLTransactionalFunction<R> command) {
		return queryExecutor.queryWithRetry(retryCount, command);
	}

	@Override
	public int clearExpiredPunishments(DSLContext context, PunishmentType type, Instant currentTime) {
		assert type != PunishmentType.KICK;
		var dataTable = new TableForType(type).dataTable();
		return context
				.deleteFrom(dataTable.table())
				.where(dataTable.id().in(context
						.select(PUNISHMENTS.ID)
						.from(PUNISHMENTS)
						.where(PUNISHMENTS.END.notEqual(Instant.MAX))
						.and(PUNISHMENTS.END.lessThan(currentTime))
				))
				.execute();
	}

	@Override
	public void truncateAllTables() {
		execute((context) -> {
			for (Table<?> table : DatabaseConstants.allTables(DatabaseConstants.TableOrder.REFERENTS_LAST)) {
				context.deleteFrom(table).execute();
			}
		}).join();
	}

	@Override
	public boolean checkResetLocalDatabaseCompact() {
        assert vendor == Vendor.HSQLDB : "Not supported by " + vendor;

		boolean needsCompact;
		// Keep track of the date of the last compaction
		Path storeCompactAt = folder.resolve("internal").resolve("last_database_compact");
		try {
			Instant currentTime = Instant.now();
			Instant lastTime;
			if (Files.exists(storeCompactAt)) {
				lastTime = Instant.ofEpochSecond(ByteBuffer.wrap(Files.readAllBytes(storeCompactAt)).getLong());
				needsCompact = Duration.between(lastTime, currentTime).toDays() >= 30L;
			} else {
				needsCompact = true;
			}
			if (needsCompact) {
				Files.write(storeCompactAt, ByteBuffer.allocate(Long.BYTES).putLong(currentTime.getEpochSecond()).array());
			}
		} catch (IOException | BufferUnderflowException ex) {
			logger.warn("Unable to update {}", storeCompactAt, ex);
			needsCompact = false;
		}
		return needsCompact;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	private class External implements PunishmentDatabase {

		@Override
		public Connection getConnection() throws SQLException {
			logger.debug("Foreign caller acquiring connection");
			return StandardDatabase.this.getConnection();
		}

		@Override
		public int getMajorRevision() {
			return PluginInfo.DATABASE_REVISION_MAJOR;
		}

		@Override
		public int getMinorRevision() {
			return PluginInfo.DATABASE_REVISION_MINOR;
		}

		@Override
		public Executor getExecutor() {
			return threadPool;
		}

	}

}
