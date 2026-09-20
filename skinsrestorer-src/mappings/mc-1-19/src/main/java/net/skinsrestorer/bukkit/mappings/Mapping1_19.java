/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.skinsrestorer.bukkit.mappings;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.biome.BiomeManager;
import net.skinsrestorer.viaversion.ViaRefreshProvider;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

public class Mapping1_19 implements IMapping {
    @Override
    public void accept(Player player, ViaRefreshProvider viaFunction) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();

        // Slowly getting from object to object till we get what is needed for
        // the respawn packet
        ServerLevel serverLevel = serverPlayer.getLevel();
        ServerPlayerGameMode gamemode = serverPlayer.gameMode;

        ClientboundRespawnPacket respawn = new ClientboundRespawnPacket(
                serverLevel.dimensionTypeId(),
                serverLevel.dimension(),
                BiomeManager.obfuscateSeed(serverLevel.getSeed()),
                gamemode.getGameModeForPlayer(),
                gamemode.getPreviousGameModeForPlayer(),
                serverLevel.isDebug(),
                serverLevel.isFlat(),
                true,
                serverPlayer.getLastDeathLocation()
        );

        resendInfoPackets(player, player);

        if (viaFunction.test(() -> IMapping.newViaPacketData(player, respawn.getSeed(), respawn.getPlayerGameType().getId(), respawn.isFlat()))) {
            serverPlayer.connection.send(respawn);
        }

        serverPlayer.onUpdateAbilities();

        serverPlayer.connection.teleport(player.getLocation());

        // Send health, food, experience (food is sent together with health)
        serverPlayer.resetSentInfo();

        PlayerList playerList = serverLevel.getServer().getPlayerList();
        playerList.sendPlayerPermissionLevel(serverPlayer);
        playerList.sendLevelInfo(serverPlayer, serverLevel);
        playerList.sendAllPlayerInfo(serverPlayer);

        // Resend their effects
        for (MobEffectInstance mobEffect : serverPlayer.getActiveEffects()) {
            serverPlayer.connection.send(new ClientboundUpdateMobEffectPacket(serverPlayer.getId(), mobEffect));
        }
    }

    @Override
    public void resendInfoPackets(Player toResend, Player toSendTo) {
        ServerPlayer toResendInternal = ((CraftPlayer) toResend).getHandle();
        ServerPlayer toSendToInternal = ((CraftPlayer) toSendTo).getHandle();

        toSendToInternal.connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, List.of(toResendInternal)));
        toSendToInternal.connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, List.of(toResendInternal)));
    }

    @Override
    public Set<String> getSpigotMappingVersions() {
        return Set.of(
                "7b9de0da1357e5b251eddde9aa762916" // 1.19
        );
    }
}
