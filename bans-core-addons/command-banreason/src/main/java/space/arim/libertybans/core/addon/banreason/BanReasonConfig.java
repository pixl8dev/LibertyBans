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

import net.kyori.adventure.text.Component;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.libertybans.core.addon.AddonConfig;

public interface BanReasonConfig extends AddonConfig {

	@ConfKey("no-permission")
	@ConfDefault.DefaultString("You do not have permission to trace ban reasons.")
	Component noPermission();

	@ConfDefault.DefaultString("Usage: &6/libertybans banreason <player>")
	Component usage();

	@ConfKey("player-does-not-exist")
	@ConfDefault.DefaultString("That player does not exist.")
	Component doesNotExist();

	@ConfKey("not-banned")
	@ConfComments("Shown when the player is not banned, neither directly nor through any alt account or IP.")
	@ConfDefault.DefaultString("&6%TARGET%&f is not banned, directly or through any linked account.")
	ComponentText notBanned();

	@ConfKey("direct-ban")
	@ConfComments({
			"Shown when the player themselves is the account that is banned.",
			"All standard punishment variables are available, e.g. %VICTIM%, %REASON%, %OPERATOR%, %TIME_REMAINING%."
	})
	@ConfDefault.DefaultStrings({
			"&6%TARGET%&f is banned directly (this is the root cause).",
			"Reason: &6%REASON%&f",
			"Operator: &6%OPERATOR%&f",
			"Time remaining: &6%TIME_REMAINING%&f"
	})
	ComponentText directBan();

	@ConfKey("alt-ban")
	@ConfComments({
			"Shown when the player is not banned directly, but is blocked because of a ban on a",
			"linked account or IP address. %VICTIM% is the root cause: the account/address actually banned.",
			"All standard punishment variables are available, e.g. %VICTIM%, %REASON%, %OPERATOR%, %TIME_REMAINING%."
	})
	@ConfDefault.DefaultStrings({
			"&6%TARGET%&f is not banned directly.",
			"They are blocked because of a ban on &6%VICTIM%&f (the root cause).",
			"Reason: &6%REASON%&f",
			"Operator: &6%OPERATOR%&f",
			"Time remaining: &6%TIME_REMAINING%&f"
	})
	ComponentText altBan();
}
