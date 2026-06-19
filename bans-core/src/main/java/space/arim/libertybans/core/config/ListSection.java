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
package space.arim.libertybans.core.config;

import java.util.Locale;

import net.kyori.adventure.text.Component;
import space.arim.api.jsonchat.adventure.util.ComponentText;

import space.arim.dazzleconf.annote.ConfDefault.DefaultInteger;
import space.arim.dazzleconf.annote.ConfDefault.DefaultString;
import space.arim.dazzleconf.annote.ConfDefault.DefaultStrings;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.IntegerRange;
import space.arim.dazzleconf.annote.SubSection;
import space.arim.libertybans.core.env.CmdSender;

@ConfHeader("Used for /banlist, /mutelist, /history, /warns, /blame")
public interface ListSection {

	interface PunishmentList {
		
		Component usage();
		
		int perPage();
		
		ComponentText noPages();
		
		ComponentText maxPages();
		
		Component permissionCommand();
		
		ComponentText layoutHeader();
		
		ComponentText layoutBody();
		
		ComponentText layoutFooter();
		
	}
	
	interface BanList extends PunishmentList {
		
		@Override
		@DefaultString("Usage: &6/banlist [page]")
		Component usage();

		@Override
		@IntegerRange(min = 1)
		@DefaultInteger(10)
		int perPage();

		@Override
		@DefaultString("There are no active bans.")
		ComponentText noPages();

		@Override
		@DefaultString("Page &6%PAGE%&f does not exist.")
		ComponentText maxPages();

		@Override
		@ConfKey("permission.command")
		@DefaultString("You may not view the banlist.")
		Component permissionCommand();

		@Override
		@ConfKey("layout.header")
		@DefaultStrings({"[&6ID&f] &6Subject&f",
				"Operator / Reason / Time Remaining"})
		ComponentText layoutHeader();

		@Override
		@ConfKey("layout.body")
		@DefaultStrings({"[&6%ID%&f] &6%VICTIM%&f",
				"Operator: &6%OPERATOR%&f / Reason: &6%REASON%&f / Time: &6%TIME_REMAINING%"})
		ComponentText layoutBody();

		@Override
		@ConfKey("layout.footer")
		@DefaultStrings({"Page: &6%PAGE%&f.||ttp:Click for next page||cmd:/libertybans banlist %NEXTPAGE%"})
		ComponentText layoutFooter();
		
	}
	
	interface MuteList extends PunishmentList {
		
		@Override
		@DefaultString("Usage: &6/mutelist [page]")
		Component usage();

		@Override
		@IntegerRange(min = 1)
		@DefaultInteger(10)
		int perPage();

		@Override
		@DefaultString("There are no active mutes.")
		ComponentText noPages();

		@Override
		@DefaultString("Page &6%PAGE%&f does not exist.")
		ComponentText maxPages();

		@Override
		@ConfKey("permission.command")
		@DefaultString("You may not view the mutelist.")
		Component permissionCommand();

		@Override
		@ConfKey("layout.header")
		@DefaultStrings({"[&6ID&f] &6Subject&f",
				"Operator / Reason / Time Remaining"})
		ComponentText layoutHeader();

		@Override
		@ConfKey("layout.body")
		@DefaultStrings({"[&6%ID%&f] &6%VICTIM%&f",
				"Operator: &6%OPERATOR%&f / Reason: &6%REASON%&f / Time: &6%TIME_REMAINING%"})
		ComponentText layoutBody();

		@Override
		@ConfKey("layout.footer")
		@DefaultStrings({"Page: &6%PAGE%&f.||ttp:Click for next page||cmd:/libertybans mutelist %NEXTPAGE%"})
		ComponentText layoutFooter();
		
	}
	
	interface History extends PunishmentList {
		
		@Override
		@DefaultString("Usage: &6/history <player> [page]")
		Component usage();

		@Override
		@IntegerRange(min = 1)
		@DefaultInteger(10)
		int perPage();

		@Override
		@DefaultString("&6%TARGET%&f has no history.")
		ComponentText noPages();

		@Override
		@DefaultString("Page &6%PAGE%&f does not exist.")
		ComponentText maxPages();

		@Override
		@ConfKey("permission.command")
		@DefaultString("You may not view history.")
		Component permissionCommand();

		@Override
		@ConfKey("layout.header")
		@DefaultStrings({"[&6ID&f] &6Punishment Type&f",
				"Operator / Reason / Date Enacted"})
		ComponentText layoutHeader();

		@Override
		@ConfKey("layout.body")
		@DefaultStrings({"[&6%ID%&f] &6%TYPE%&f",
				"Operator: &6%OPERATOR%&f / Reason: &6%REASON%&f / Date: &6%START_DATE%"})
		ComponentText layoutBody();

		@Override
		@ConfKey("layout.footer")
		@DefaultStrings({"Page: &6%PAGE%&f.||ttp:Click for next page||cmd:/libertybans history %TARGET% %NEXTPAGE%"})
		ComponentText layoutFooter();
		
	}
	
	interface Warns extends PunishmentList {
		
		@Override
		@DefaultString("Usage: &6/warns <player> [page]")
		Component usage();

		@Override
		@IntegerRange(min = 1)
		@DefaultInteger(10)
		int perPage();

		@Override
		@DefaultString("&6%TARGET%&f has no warns.")
		ComponentText noPages();

		@Override
		@DefaultString("Page &6%PAGE%&f does not exist.")
		ComponentText maxPages();

		@Override
		@ConfKey("permission.command")
		@DefaultString("You may not view warns.")
		Component permissionCommand();

		@Override
		@ConfKey("layout.header")
		@DefaultStrings({"[&6ID&f] &6Operator &f/ &6Reason &f/ &6Time Remaining"})
		ComponentText layoutHeader();

		@Override
		@ConfKey("layout.body")
		@DefaultStrings({"[&6%ID%&f] &6%OPERATOR%&f / &6%REASON%&f / &6%TIME_REMAINING%"})
		ComponentText layoutBody();

		@Override
		@ConfKey("layout.footer")
		@DefaultStrings({"Page: &6%PAGE%&f.||ttp:Click for next page||cmd:/libertybans warns %TARGET% %NEXTPAGE%"})
		ComponentText layoutFooter();
		
	}
	
	interface Blame extends PunishmentList {
		
		@Override
		@DefaultString("Usage: &6/blame <player> [page]")
		Component usage();

		@Override
		@IntegerRange(min = 1)
		@DefaultInteger(10)
		int perPage();

		@Override
		@DefaultString("&6%TARGET%&f has not punished any players.")
		ComponentText noPages();

		@Override
		@DefaultString("Page &6%PAGE%&f does not exist.")
		ComponentText maxPages();

		@Override
		@ConfKey("permission.command")
		@DefaultString("You may not use blame.")
		Component permissionCommand();

		@Override
		@ConfKey("layout.header")
		@DefaultStrings({"[&6ID&f] &6Subject / Punishment Type&f",
				"Reason / Date Enacted"})
		ComponentText layoutHeader();

		@Override
		@ConfKey("layout.body")
		@DefaultStrings({"[&6%ID%&f] &6%VICTIM%&f / &6%TYPE%&f",
				"Reason: &6%REASON%&f / Date: &6%START_DATE%"})
		ComponentText layoutBody();

		@Override
		@ConfKey("layout.footer")
		@DefaultStrings({"Page: &6%PAGE%&f.||ttp:Click for next page||cmd:/libertybans blame %TARGET% %NEXTPAGE%"})
		ComponentText layoutFooter();
		
	}
	
	enum ListType {
		
		BANLIST,
		MUTELIST,
		HISTORY,
		WARNS,
		BLAME;
		
		public boolean requiresTarget() {
			switch (this) {
			case BANLIST:
			case MUTELIST:
				return false;
			case HISTORY:
			case WARNS:
			case BLAME:
				return true;
			default:
				throw new IllegalArgumentException("requiresTarget not up-to-date");
			}
		}

		public boolean hasPermission(CmdSender sender) {
			return sender.hasPermission("libertybans.list." + this);
		}

		public static ListType fromString(String listType) {
			return valueOf(listType.toUpperCase(Locale.ROOT));
		}

		@Override
		public String toString() {
			return name().toLowerCase(Locale.ROOT);
		}
	}
	
	default PunishmentList forType(ListType type) {
		switch (type) {
		case BANLIST:
			return banList();
		case MUTELIST:
			return muteList();
		case HISTORY:
			return history();
		case WARNS:
			return warns();
		case BLAME:
			return blame();
		default:
			throw new IllegalArgumentException("Unknown list type " + type);
		}
	}

	@ConfKey("ban-list")
	@SubSection
	BanList banList();

	@ConfKey("mute-list")
	@SubSection
	MuteList muteList();
	
	@SubSection
	History history();
	
	@SubSection
	Warns warns();
	
	@SubSection
	Blame blame();
	
}
