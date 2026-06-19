/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

package space.arim.libertybans.core.config;

import net.kyori.adventure.text.Component;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.dazzleconf.annote.ConfDefault.DefaultString;
import space.arim.dazzleconf.annote.ConfDefault.DefaultStrings;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.SubSection;
import space.arim.libertybans.api.PunishmentType;

@ConfHeader({
		"",
		"Messages regarding /ban, /mute, /warn, /kick",
		"Includes punishment layouts",
		"",
		""})
public interface AdditionsSection {

	interface BanAddition extends PunishmentAdditionSection.WithDurationPerm {

		@Override
		@DefaultString("Usage: /ban <player> [time] <reason>.")
		Component usage();

		@Override
		@DefaultString("&6%TARGET%&f cannot be banned.")
		ComponentText exempted();

		@Override
		@DefaultString("&6%TARGET%&f is already banned.")
		ComponentText conflicting();

		@Override
		@ConfKey("success.message")
		@DefaultString("Banned &6%VICTIM%&f for %DURATION% because of &6%REASON%&f.")
		ComponentText successMessage();

		@Override
		@ConfKey("success.notification")
		@DefaultString("&6%OPERATOR%&f banned &6%VICTIM%&f for %DURATION% because of &6%REASON%&f.")
		ComponentText successNotification();

		@Override
		@DefaultStrings({
				"&6BANNED&f",
				"You have been banned from FadeMC.",
				"",
				"Duration: &6%TIME_REMAINING%&f",
				"",
				"Reason: &6%REASON%&f",
				"",
				"You may appeal your punishment below:",
				"Discord: &6discord.fademc.xyz"})
		ComponentText layout();

	}

	interface MuteAddition extends PunishmentAdditionSection.WithDurationPerm {

		@Override
		@DefaultString("Usage: /mute <player> [time] <reason>.")
		Component usage();

		@Override
		@DefaultString("&6%TARGET%&f cannot be muted.")
		ComponentText exempted();

		@Override
		@DefaultString("&6%TARGET%&f is already muted.")
		ComponentText conflicting();

		@Override
		@ConfKey("success.message")
		@DefaultString("Muted &6%VICTIM%&f for %DURATION% because of &6%REASON%&f.")
		ComponentText successMessage();

		@Override
		@ConfKey("success.notification")
		@DefaultString("&6%OPERATOR%&f muted &6%VICTIM%&f for %DURATION% because of &6%REASON%&f.")
		ComponentText successNotification();

		@Override
		@DefaultStrings({
				"&6MUTED&f",
				"Duration: &6%TIME_REMAINING%&f",
				"",
				"Reason: &6%REASON%"})
		ComponentText layout();

	}

	interface WarnAddition extends PunishmentAdditionSection.WithDurationPerm {

		@Override
		@DefaultString("Usage: /warn <player> [time] <reason>.")
		Component usage();

		@Override
		@DefaultString("&6%TARGET%&f cannot be warned.")
		ComponentText exempted();

		@Override
		default ComponentText conflicting() {
			return SHOULD_NOT_CONFLICT;
		}

		@Override
		@ConfKey("success.message")
		@DefaultString("Warned &6%VICTIM%&f for %DURATION% because of &6%REASON%&f.")
		ComponentText successMessage();

		@Override
		@ConfKey("success.notification")
		@DefaultString("&6%OPERATOR%&f warned &6%VICTIM%&f for %DURATION% because of &6%REASON%&f.")
		ComponentText successNotification();

		@Override
		@DefaultStrings({
				"&6WARNED&f",
				"Duration: %TIME_REMAINING%",
				"",
				"Reason: &6%REASON%"})
		ComponentText layout();

	}

	interface KickAddition extends PunishmentAdditionSection.WithLayout {

		@Override
		@DefaultString("Usage: /kick <player> <reason>.")
		Component usage();

		@Override
		@DefaultString("&6%TARGET%&f cannot be kicked.")
		ComponentText exempted();

		@Override
		default ComponentText conflicting() {
			return SHOULD_NOT_CONFLICT;
		}

		@Override
		@ConfKey("success.message")
		@DefaultString("Kicked &6%VICTIM%&f because of &6%REASON%&f.")
		ComponentText successMessage();

		@Override
		@ConfKey("success.notification")
		@DefaultString("&6%OPERATOR%&f kicked &6%VICTIM%&f because of &6%REASON%&f.")
		ComponentText successNotification();

		@Override
		@DefaultStrings({
				"&6KICKED&f",
				"",
				"Reason: &6%REASON%"})
		ComponentText layout();

		@ConfKey("must-be-online")
		@DefaultString("&6%TARGET%&f must be online.")
		ComponentText mustBeOnline();

	}
	
	@SubSection
	BanAddition bans();
	
	@SubSection
	MuteAddition mutes();
	
	@SubSection
	WarnAddition warns();
	
	@SubSection
	KickAddition kicks();

	default PunishmentAdditionSection.WithLayout forType(PunishmentType type) {
		return switch (type) {
			case BAN -> bans();
			case MUTE -> mutes();
			case WARN -> warns();
			case KICK -> kicks();
		};
	}

}
