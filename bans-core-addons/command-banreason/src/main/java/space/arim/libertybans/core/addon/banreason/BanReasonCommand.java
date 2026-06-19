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

package space.arim.libertybans.core.addon.banreason;

import jakarta.inject.Inject;
import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.api.select.SortPunishments;
import space.arim.libertybans.core.commands.AbstractCommandExecution;
import space.arim.libertybans.core.commands.AbstractSubCommandGroup;
import space.arim.libertybans.core.commands.CommandExecution;
import space.arim.libertybans.core.commands.CommandPackage;
import space.arim.libertybans.core.commands.extra.TabCompletion;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.UUIDAndAddress;
import space.arim.libertybans.core.uuid.UUIDManager;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.util.stream.Stream;

public final class BanReasonCommand extends AbstractSubCommandGroup {

	private final PunishmentSelector selector;
	private final UUIDManager uuidManager;
	private final InternalFormatter formatter;
	private final TabCompletion tabCompletion;
	private final BanReasonAddon banReasonAddon;

	@Inject
	public BanReasonCommand(Dependencies dependencies,
							PunishmentSelector selector, UUIDManager uuidManager, InternalFormatter internalFormatter,
							TabCompletion tabCompletion, BanReasonAddon banReasonAddon) {
		super(dependencies, "banreason");
		this.selector = selector;
		this.uuidManager = uuidManager;
		this.formatter = internalFormatter;
		this.tabCompletion = tabCompletion;
		this.banReasonAddon = banReasonAddon;
	}

	@Override
	public CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
		return new Execution(sender, command);
	}

	@Override
	public Stream<String> suggest(CmdSender sender, String arg, int argIndex) {
		if (argIndex == 0) {
			return tabCompletion.completeOfflinePlayerNames(sender);
		}
		return Stream.empty();
	}

	@Override
	public boolean hasTabCompletePermission(CmdSender sender, String arg) {
		return hasPermission(sender);
	}

	private boolean hasPermission(CmdSender sender) {
		return sender.hasPermission("libertybans.addon.banreason.use");
	}

	private final class Execution extends AbstractCommandExecution {

		private final BanReasonConfig config;

		private Execution(CmdSender sender, CommandPackage command) {
			super(sender, command);
			config = banReasonAddon.config();
		}

		@Override
		public @Nullable ReactionStage<Void> execute() {
			if (!hasPermission(sender())) {
				sender().sendMessage(config.noPermission());
				return null;
			}
			if (!command().hasNext()) {
				sender().sendMessage(config.usage());
				return null;
			}
			String name = command().next();

			return uuidManager.lookupPlayer(name).thenCompose((optUuidAddress) -> {
				if (optUuidAddress.isEmpty()) {
					sender().sendMessage(config.doesNotExist());
					return completedFuture(null);
				}
				UUIDAndAddress uuidAddress = optUuidAddress.get();
				// Trace the root cause using the server's configured address strictness, so that the
				// reported ban reflects what actually applies to the player at login.
				return selector.selectionByApplicabilityBuilder(uuidAddress.uuid(), uuidAddress.address())
						.type(PunishmentType.BAN)
						.build()
						.getFirstSpecificPunishment(SortPunishments.LATEST_END_DATE_FIRST)
						.thenCompose((optPunishment) -> {
							if (optPunishment.isEmpty()) {
								sender().sendMessage(config.notBanned().replaceText("%TARGET%", name));
								return completedFuture(null);
							}
							Punishment punishment = optPunishment.get();
							ComponentText layout = isDirectBan(punishment.getVictim(), uuidAddress)
									? config.directBan() : config.altBan();
							return formatter.formatWithPunishment(layout.replaceText("%TARGET%", name), punishment)
									.thenAccept(sender()::sendMessage);
						});
			});
		}

		private boolean isDirectBan(Victim victim, UUIDAndAddress target) {
			return victim instanceof PlayerVictim playerVictim
					&& playerVictim.getUUID().equals(target.uuid());
		}
	}
}
