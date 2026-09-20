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
package net.skinsrestorer.codec;

import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.codec.NetworkCodec;
import net.skinsrestorer.shared.codec.SRInputReader;
import net.skinsrestorer.shared.codec.SROutputWriter;
import net.skinsrestorer.shared.codec.SRProxyPluginMessage;
import net.skinsrestorer.shared.codec.SRProxyPluginMessage.GUIActionChannelPayload;
import net.skinsrestorer.shared.codec.SRProxyPluginMessage.GUIActionListChannelPayload;
import net.skinsrestorer.shared.codec.SRServerPluginMessage;
import net.skinsrestorer.shared.gui.PageType;
import net.skinsrestorer.shared.subjects.messages.ComponentString;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PluginMessageCodecTest {
    private <T> T roundTrip(NetworkCodec<T> codec, T value) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        SROutputWriter writer = new SROutputWriter(dos);
        codec.write(writer, value);
        SRInputReader reader = new SRInputReader(baos.toByteArray());
        return codec.read(reader);
    }

    // --- SRProxyPluginMessage tests ---

    @Test
    void proxyAckMessage() {
        UUID ackId = UUID.randomUUID();
        var payload = new SRProxyPluginMessage.AckChannelPayload(ackId, "1.0.0");
        var message = new SRProxyPluginMessage(payload);

        var result = roundTrip(SRProxyPluginMessage.CODEC, message);
        assertInstanceOf(SRProxyPluginMessage.AckChannelPayload.class, result.channelPayload());
        var ack = (SRProxyPluginMessage.AckChannelPayload) result.channelPayload();
        assertEquals(ackId, ack.ackId());
        assertEquals("1.0.0", ack.serverSrVersion());
    }

    @Test
    void proxySetSkinAction() {
        SkinIdentifier skin = SkinIdentifier.ofCustom("myskin");
        var action = new GUIActionChannelPayload(new GUIActionChannelPayload.SetSkinPayload(skin));
        var payload = new GUIActionListChannelPayload(List.of(action));
        var message = new SRProxyPluginMessage(payload);

        var result = roundTrip(SRProxyPluginMessage.CODEC, message);
        assertInstanceOf(GUIActionListChannelPayload.class, result.channelPayload());
        var list = (GUIActionListChannelPayload) result.channelPayload();
        assertEquals(1, list.actions().size());
        assertInstanceOf(GUIActionChannelPayload.SetSkinPayload.class, list.actions().getFirst().payload());
        var setSkin = (GUIActionChannelPayload.SetSkinPayload) list.actions().getFirst().payload();
        assertEquals(skin, setSkin.skinIdentifier());
    }

    @Test
    void proxyClearSkinAction() {
        var action = new GUIActionChannelPayload(GUIActionChannelPayload.ClearSkinPayload.INSTANCE);
        var payload = new GUIActionListChannelPayload(List.of(action));
        var message = new SRProxyPluginMessage(payload);

        var result = roundTrip(SRProxyPluginMessage.CODEC, message);
        var list = (GUIActionListChannelPayload) result.channelPayload();
        assertInstanceOf(GUIActionChannelPayload.ClearSkinPayload.class, list.actions().getFirst().payload());
    }

    @Test
    void proxyOpenPageAction() {
        var action = new GUIActionChannelPayload(new GUIActionChannelPayload.OpenPagePayload(3, PageType.FAVOURITES));
        var payload = new GUIActionListChannelPayload(List.of(action));
        var message = new SRProxyPluginMessage(payload);

        var result = roundTrip(SRProxyPluginMessage.CODEC, message);
        var list = (GUIActionListChannelPayload) result.channelPayload();
        var openPage = (GUIActionChannelPayload.OpenPagePayload) list.actions().getFirst().payload();
        assertEquals(3, openPage.page());
        assertEquals(PageType.FAVOURITES, openPage.type());
    }

    @Test
    void proxyAddFavouriteAction() {
        SkinIdentifier skin = SkinIdentifier.ofCustom("fav");
        var action = new GUIActionChannelPayload(new GUIActionChannelPayload.AddFavouritePayload(skin));
        var payload = new GUIActionListChannelPayload(List.of(action));
        var message = new SRProxyPluginMessage(payload);

        var result = roundTrip(SRProxyPluginMessage.CODEC, message);
        var list = (GUIActionListChannelPayload) result.channelPayload();
        var addFav = (GUIActionChannelPayload.AddFavouritePayload) list.actions().getFirst().payload();
        assertEquals(skin, addFav.skinIdentifier());
    }

    @Test
    void proxyRemoveFavouriteAction() {
        SkinIdentifier skin = SkinIdentifier.ofCustom("fav");
        var action = new GUIActionChannelPayload(new GUIActionChannelPayload.RemoveFavouritePayload(skin));
        var payload = new GUIActionListChannelPayload(List.of(action));
        var message = new SRProxyPluginMessage(payload);

        var result = roundTrip(SRProxyPluginMessage.CODEC, message);
        var list = (GUIActionListChannelPayload) result.channelPayload();
        var removeFav = (GUIActionChannelPayload.RemoveFavouritePayload) list.actions().getFirst().payload();
        assertEquals(skin, removeFav.skinIdentifier());
    }

    @Test
    void proxyMultipleActions() {
        var actions = List.of(
                new GUIActionChannelPayload(GUIActionChannelPayload.ClearSkinPayload.INSTANCE),
                new GUIActionChannelPayload(new GUIActionChannelPayload.SetSkinPayload(SkinIdentifier.ofCustom("test"))),
                new GUIActionChannelPayload(new GUIActionChannelPayload.OpenPagePayload(1, PageType.MAIN))
        );
        var payload = new GUIActionListChannelPayload(actions);
        var message = new SRProxyPluginMessage(payload);

        var result = roundTrip(SRProxyPluginMessage.CODEC, message);
        var list = (GUIActionListChannelPayload) result.channelPayload();
        assertEquals(3, list.actions().size());
    }

    // --- SRServerPluginMessage tests ---

    @Test
    void serverSkinUpdateV2() {
        SkinProperty prop = SkinProperty.of("value", "signature");
        var payload = new SRServerPluginMessage.SkinUpdateV2ChannelPayload(prop);
        var message = new SRServerPluginMessage(payload);

        var result = roundTrip(SRServerPluginMessage.CODEC, message);
        assertInstanceOf(SRServerPluginMessage.SkinUpdateV2ChannelPayload.class, result.channelPayload());
        var update = (SRServerPluginMessage.SkinUpdateV2ChannelPayload) result.channelPayload();
        assertEquals(prop, update.skinProperty());
    }

    @Test
    void serverSkinUpdateV3WithAck() {
        SkinProperty prop = SkinProperty.of("val", "sig");
        UUID ackId = UUID.randomUUID();
        var ack = new SRServerPluginMessage.SkinUpdateV3ChannelPayload.AckPayload(ackId, "2.0.0");
        var payload = new SRServerPluginMessage.SkinUpdateV3ChannelPayload(prop, Optional.of(ack));
        var message = new SRServerPluginMessage(payload);

        var result = roundTrip(SRServerPluginMessage.CODEC, message);
        var update = (SRServerPluginMessage.SkinUpdateV3ChannelPayload) result.channelPayload();
        assertEquals(prop, update.skinProperty());
        assertTrue(update.ackPayload().isPresent());
        assertEquals(ackId, update.ackPayload().get().ackId());
        assertEquals("2.0.0", update.ackPayload().get().proxySrVersion());
    }

    @Test
    void serverSkinUpdateV3WithoutAck() {
        SkinProperty prop = SkinProperty.of("val", "sig");
        var payload = new SRServerPluginMessage.SkinUpdateV3ChannelPayload(prop, Optional.empty());
        var message = new SRServerPluginMessage(payload);

        var result = roundTrip(SRServerPluginMessage.CODEC, message);
        var update = (SRServerPluginMessage.SkinUpdateV3ChannelPayload) result.channelPayload();
        assertEquals(prop, update.skinProperty());
        assertTrue(update.ackPayload().isEmpty());
    }

    @Test
    void serverGiveSkull() {
        var displayName = new ComponentString("{\"text\":\"Cool Skull\"}");
        var payload = new SRServerPluginMessage.GiveSkullChannelPayload(displayName, "abc123hash");
        var message = new SRServerPluginMessage(payload);

        var result = roundTrip(SRServerPluginMessage.CODEC, message);
        assertInstanceOf(SRServerPluginMessage.GiveSkullChannelPayload.class, result.channelPayload());
        var skull = (SRServerPluginMessage.GiveSkullChannelPayload) result.channelPayload();
        assertEquals(displayName, skull.displayName());
        assertEquals("abc123hash", skull.textureHash());
    }
}
