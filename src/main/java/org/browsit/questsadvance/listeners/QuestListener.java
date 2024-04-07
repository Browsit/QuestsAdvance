/*
 * Copyright (c) 2026 Browsit, LLC. All rights reserved.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN
 * NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.browsit.questsadvance.listeners;

import me.pikamug.quests.events.plugin.BukkitQuestsPluginPostReloadEvent;
import me.pikamug.quests.events.quester.BukkitQuesterPostCompleteQuestEvent;
import me.pikamug.quests.events.quester.BukkitQuesterPostFailQuestEvent;
import me.pikamug.quests.events.quester.BukkitQuesterPostStartQuestEvent;
import me.pikamug.quests.events.quester.BukkitQuesterPostUpdateObjectiveEvent;
import me.pikamug.quests.player.BukkitQuester;
import me.pikamug.quests.quests.Quest;
import me.pikamug.quests.quests.components.Objective;
import org.browsit.questsadvance.advancements.AdvanceManager;
import org.browsit.questsadvance.QuestsAdvance;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class QuestListener implements Listener {

    private final QuestsAdvance plugin;

    public QuestListener(final QuestsAdvance plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuestStart(final BukkitQuesterPostStartQuestEvent event) {
        final BukkitQuester quester = event.getQuester();
        final Player player = quester.getPlayer();
        if (player == null) return;

        final AdvanceManager manager = plugin.getAdvancementManager();
        manager.showQuestToPlayer(player);
        manager.updateVisibilityForPlayer(player);
    }

    @EventHandler
    public void onQuestComplete(final BukkitQuesterPostCompleteQuestEvent event) {
        final BukkitQuester quester = event.getQuester();
        final Player player = quester.getPlayer();
        if (player == null) return;

        plugin.getAdvancementManager().updateVisibilityForPlayer(player);
    }

    @EventHandler
    public void onQuestFail(final BukkitQuesterPostFailQuestEvent event) {
        final BukkitQuester quester = event.getQuester();
        final Player player = quester.getPlayer();
        if (player == null) return;

        plugin.getAdvancementManager().updateVisibilityForPlayer(player);
    }

    @EventHandler
    public void onObjectiveUpdate(final BukkitQuesterPostUpdateObjectiveEvent event) {
        final Quest quest = event.getQuest();
        final BukkitQuester quester = event.getQuester();
        final Objective objective = event.getObjective();

        plugin.getAdvancementManager().updateObjectiveProgression(quest, quester, objective);
    }

    @EventHandler
    public void onQuestReload(final BukkitQuestsPluginPostReloadEvent event) {
        if (!event.isSuccess()) return;

        final AdvanceManager manager = plugin.getAdvancementManager();
        manager.unregisterQuestsTab();
        manager.setupAllQuestAdvancements();

        for (final Player player : Bukkit.getOnlinePlayers()) {
            manager.showQuestToPlayer(player);
        }
    }
}
