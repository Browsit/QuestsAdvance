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

package org.browsit.questsadvance.advancements;

import com.fren_gor.ultimateAdvancementAPI.AdvancementTab;
import com.fren_gor.ultimateAdvancementAPI.UltimateAdvancementAPI;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.RootAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.exceptions.DuplicatedException;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementUtils;
import me.pikamug.quests.BukkitQuestsPlugin;
import me.pikamug.quests.Quests;
import me.pikamug.quests.enums.ObjectiveType;
import me.pikamug.quests.player.Quester;
import me.pikamug.quests.quests.BukkitQuest;
import me.pikamug.quests.quests.Quest;
import me.pikamug.quests.quests.components.BukkitStage;
import me.pikamug.quests.quests.components.Objective;
import me.pikamug.quests.quests.components.Stage;
import me.pikamug.quests.util.BukkitItemUtil;
import me.pikamug.quests.util.BukkitLang;
import me.pikamug.quests.util.BukkitMiscUtil;
import me.pikamug.quests.util.stack.BlockItemStack;
import org.browsit.questsadvance.QuestsAdvance;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class AdvanceManager {

    private static Quests staticQuests;
    private static final Map<AdvancementKey, Quest> advancementQuestMap = new HashMap<>();

    private final UltimateAdvancementAPI api;
    private final QuestsAdvance plugin;

    private AdvancementTab questTab;
    private final Map<String, List<ObjectiveAdvancement>> questAdvancements = new HashMap<>();

    static class ObjectiveAdvancement {
        final Advancement advancement;
        final ObjectiveType type;
        final String goalObjectStr;
        final String message;
        final int goal;

        ObjectiveAdvancement(final Advancement advancement, final ObjectiveType type, final String goalObjectStr, final String message, final int goal) {
            this.advancement = advancement;
            this.type = type;
            this.goalObjectStr = goalObjectStr;
            this.message = message;
            this.goal = goal;
        }
    }

    private static class QuestsAdvancement extends BaseAdvancement {
        QuestsAdvancement(final String key, final AdvancementDisplay display, final RootAdvancement parent, final int goal) {
            super(key, display, parent, goal);
        }

        @Override
        public void giveReward(@NotNull final Player player) {
            // We let Quests handle rewards
        }

        @Override
        public void onGrant(@NotNull final Player player, final boolean giveReward) {
            final AdvancementTab tab = getAdvancementTab();
            final Plugin p = tab.getOwningPlugin();
            super.onGrant(player, giveReward);
            Bukkit.getScheduler().scheduleSyncDelayedTask(p, () -> showToast(player), 1L);
        }

        private void showToast(@NotNull final Player player) {
            if (!getDisplay().doesShowToast()) return;

            final ItemStack icon = getDisplay().getIcon();
            if (icon.getType() == Material.AIR) return;

            final String title = getDisplay().getTitle();
            final AdvancementFrameType frame = getDisplay().getFrame();

            AdvancementUtils.displayToast(player, icon, title, frame);
        }

        @Override
        public boolean isVisible(final TeamProgression progression) {
            final UUID playerUuid = progression.getAMember();
            if (playerUuid == null) return false;

            final Quest quest = getQuestForAdvancement(this);
            if (quest == null) return false;

            final Quester quester = staticQuests.getQuester(playerUuid);
            if (quester == null) return false;

            final Map<Quest, Integer> current = quester.getCurrentQuests();
            if (current.containsKey(quest)) return true;

            final Collection<Quest> completed = quester.getCompletedQuests();
            if (completed.contains(quest)) return false;

            return quester.canAcceptOffer(quest, false) && quest.testRequirements(quester);
        }
    }

    public AdvanceManager(final QuestsAdvance plugin, final Quests quests, final UltimateAdvancementAPI api) {
        this.plugin = plugin;
        this.api = api;
        staticQuests = quests;
    }

    public AdvancementTab getQuestTab() {
        return questTab;
    }

    public void setupAllQuestAdvancements() {
        final Collection<Quest> loadedQuests = staticQuests.getLoadedQuests();
        if (loadedQuests.isEmpty()) {
            plugin.getLogger().info("No quests loaded, skipping advancement tab creation.");
            return;
        }

        try {
            questTab = api.createAdvancementTab("questsadvance");
        } catch (final DuplicatedException e) {
            plugin.getLogger().severe("Duplicate advancement tab! Are multiple jar files installed?");
            return;
        }

        final RootAdvancement rootAdv = getRootAdvancement(loadedQuests);

        final Set<BaseAdvancement> allAdvs = new LinkedHashSet<>();
        int questIndex = 0;

        for (final Quest quest : loadedQuests) {
            final ItemStack icon = getQuestIcon((BukkitQuest) quest);
            final List<ObjectiveAdvancement> entries = new ArrayList<>();
            final List<Stage> stages = quest.getStages();
            int objIndex = 0;
            int stageIndex = 0;
            for (final Stage stage : stages) {
                final List<ObjectiveData> objectives = extractObjectives((BukkitStage) stage, stageIndex+1);

                for (final ObjectiveData od : objectives) {
                    final AdvancementFrameType frameType = stages.size() == stageIndex+1
                            ? AdvancementFrameType.CHALLENGE : AdvancementFrameType.GOAL;
                    final AdvancementDisplay display = new AdvancementDisplay(
                            icon, quest.getName(), frameType, plugin.getSettings().canShowToast(),
                            plugin.getSettings().canAnnounceChat(), 1 + objIndex, questIndex, od.description);
                    final QuestsAdvancement adv = new QuestsAdvancement(
                            quest.getId().toLowerCase() + "_s" + stages.indexOf(stage) + "_o" + objIndex,
                            display, rootAdv, od.goal);
                    advancementQuestMap.put(adv.getKey(), quest);
                    allAdvs.add(adv);
                    entries.add(new ObjectiveAdvancement(
                            adv, od.type, od.identifier, od.description, od.goal));
                    objIndex++;
                }
                stageIndex++;
            }

            questAdvancements.put(quest.getId().toLowerCase(), entries);
            questIndex++;
        }

        questTab.registerAdvancements(rootAdv, allAdvs);
        plugin.getLogger().info("Created " + plugin.getSettings().getRootName() + " advancement tab with "
                + allAdvs.size() + " objectives across " + questIndex + " quests.");
    }

    private ItemStack getQuestIcon(final BukkitQuest quest) {
        final ItemStack display = quest.getGUIDisplay();
        if (display != null) {
            return display;
        }
        return new ItemStack(Material.PAPER);
    }

    private @NotNull RootAdvancement getRootAdvancement(final Collection<Quest> loadedQuests) {
        final int questCount = loadedQuests.size();
        final float rootY = (questCount - 1) / 2.0f;
        final ItemStack rootIcon = new ItemStack(Material.WRITTEN_BOOK);
        final AdvancementDisplay rootDisplay = new AdvancementDisplay(
                rootIcon, plugin.getSettings().getRootName(), AdvancementFrameType.TASK,
                false, false, 0, rootY, "Quest objectives and progress");
        return new RootAdvancement(questTab, "root", rootDisplay, "textures/block/"
                + plugin.getSettings().getBackground() + ".png");
    }

    public void unregisterQuestsTab() {
        if (questTab != null) {
            final String ns = questTab.getNamespace();
            if (api.isAdvancementTabRegistered(ns)) {
                api.unregisterAdvancementTab(ns);
            }
            questTab = null;
        }
        questAdvancements.clear();
        advancementQuestMap.clear();
    }

    public void showQuestToPlayer(final Player player) {
        if (questTab == null) {
            setupAllQuestAdvancements();
        }
        if (questTab != null) {
            questTab.showTab(player);
            questTab.grantRootAdvancement(player);
        }
    }

    public void updateVisibilityForPlayer(final Player player) {
        if (questTab != null) {
            questTab.updateAdvancementsToTeam(player);
        }
    }

    static Quest getQuestForAdvancement(final Advancement advancement) {
        return advancementQuestMap.get(advancement.getKey());
    }

    public void updateObjectiveProgression(final Quest quest, final Quester quester, final Objective objective) {
        final List<ObjectiveAdvancement> entries = questAdvancements.get(quest.getId().toLowerCase());
        if (entries == null) return;

        final ObjectiveType type = objective.getType();
        final int goal = objective.getGoal();
        final int progress = objective.getProgress();

        int idx = 0;
        for (final Stage rawStage : quest.getStages()) {
            final BukkitStage stage = (BukkitStage) rawStage;
            final List<ObjectiveData> objectives = extractObjectives(stage, 0);
            for (final ObjectiveData od : objectives) {
                if (od.type == type && od.goal == goal) {
                    if (idx < entries.size()) {
                        entries.get(idx).advancement.setProgression(
                                quester.getUUID(), progress);
                    }
                    return;
                }
                idx++;
            }
        }
    }

    public void removeQuestById(final String questId) {
        questAdvancements.remove(questId.toLowerCase());
    }

    private List<ObjectiveData> extractObjectives(final BukkitStage stage, final int stageNum) {
        final List<ObjectiveData> result = new ArrayList<>();
        final String stageInfo = ChatColor.DARK_GRAY + "[" + BukkitLang.get("stageEditorStage") + " " + stageNum + "] "
                + ChatColor.GREEN;

        for (final BlockItemStack block : stage.getBlocksToBreak()) {
            final int goal = block.getAmount();
            result.add(new ObjectiveData(ObjectiveType.BREAK_BLOCK, stageInfo +
                    BukkitLang.get("break").replace("<item>", BukkitItemUtil.getName(block))
                    .replace(": <count>", ""), goal, block.toString()));
        }
        for (final BlockItemStack block : stage.getBlocksToDamage()) {
            final int goal = block.getAmount();
            result.add(new ObjectiveData(ObjectiveType.DAMAGE_BLOCK, stageInfo +
                    BukkitLang.get("damage").replace("<item>", BukkitItemUtil.getName(block))
                    .replace(": <count>", ""), goal, block.toString()));
        }
        for (final BlockItemStack block : stage.getBlocksToPlace()) {
            final int goal = block.getAmount();
            result.add(new ObjectiveData(ObjectiveType.PLACE_BLOCK, stageInfo +
                    BukkitLang.get("place").replace("<item>", BukkitItemUtil.getName(block))
                    .replace(": <count>", ""), goal, block.toString()));
        }
        for (final BlockItemStack block : stage.getBlocksToUse()) {
            final int goal = block.getAmount();
            result.add(new ObjectiveData(ObjectiveType.USE_BLOCK, stageInfo +
                    BukkitLang.get("use").replace("<item>", BukkitItemUtil.getName(block))
                    .replace(": <count>", ""), goal, block.toString()));
        }
        for (final BlockItemStack block : stage.getBlocksToCut()) {
            final int goal = block.getAmount();
            result.add(new ObjectiveData(ObjectiveType.CUT_BLOCK, stageInfo +
                    BukkitLang.get("cut").replace("<item>", BukkitItemUtil.getName(block))
                    .replace(": <count>", ""), goal, block.toString()));
        }
        for (final ItemStack item : stage.getItemsToCraft()) {
            final int goal = item.getAmount();
            result.add(new ObjectiveData(ObjectiveType.CRAFT_ITEM, stageInfo +
                    BukkitLang.get("craftItem").replace("<item>", BukkitItemUtil.getName(item))
                    .replace(": <count>", ""), goal, item.toString()));
        }
        for (final ItemStack item : stage.getItemsToSmelt()) {
            final int goal = item.getAmount();
            result.add(new ObjectiveData(ObjectiveType.SMELT_ITEM, stageInfo +
                    BukkitLang.get("smeltItem").replace("<item>", BukkitItemUtil.getName(item))
                    .replace(": <count>", ""), goal, item.toString()));
        }
        for (final ItemStack item : stage.getItemsToEnchant()) {
            final int goal = item.getAmount();
            result.add(new ObjectiveData(ObjectiveType.ENCHANT_ITEM, stageInfo +
                    BukkitLang.get("enchItem").replace("<item>", BukkitItemUtil.getName(item))
                    .replace(": <count>", ""), goal, item.toString()));
        }
        for (final ItemStack item : stage.getItemsToBrew()) {
            final int goal = item.getAmount();
            result.add(new ObjectiveData(ObjectiveType.BREW_ITEM, stageInfo +
                    BukkitLang.get("brewItem").replace("<item>", BukkitItemUtil.getName(item))
                    .replace(": <count>", ""), goal, item.toString()));
        }
        for (final ItemStack item : stage.getItemsToConsume()) {
            final int goal = item.getAmount();
            result.add(new ObjectiveData(ObjectiveType.CONSUME_ITEM, stageInfo +
                    BukkitLang.get("consumeItem").replace("<item>", BukkitItemUtil.getName(item))
                    .replace(": <count>", ""), goal, item.toString()));
        }
        final List<ItemStack> items = stage.getItemsToDeliver();
        final List<UUID> npcs = stage.getItemDeliveryTargets();
        for (int i = 0; i < items.size() && i < npcs.size(); i++) {
            final int goal = items.get(i).getAmount();
            final UUID npc = npcs.get(i);
            result.add(new ObjectiveData(ObjectiveType.DELIVER_ITEM, stageInfo +
                    BukkitLang.get("deliver").replace("<item>", BukkitItemUtil.getName(items.get(i)))
                            .replace("<npc>", ((BukkitQuestsPlugin)plugin.getQuests()).getDependencies().getNpcName(npc))
                            .replace(": <count>", ""), goal, items.get(i).toString()));
        }
        final List<EntityType> mobs = stage.getMobsToKill();
        final List<Integer> mobCounts = stage.getMobNumToKill();
        for (int i = 0; i < mobs.size() && i < mobCounts.size(); i++) {
            final int goal = mobCounts.get(i);
            result.add(new ObjectiveData(ObjectiveType.KILL_MOB, stageInfo +
                    BukkitLang.get("kill").replace("<mob>", BukkitMiscUtil.snakeCaseToUpperCamelCase(mobs.get(i).name()))
                    .replace(": <count>", ""), goal, mobs.get(i).toString()));
        }
        final List<UUID> npcsToInteract = stage.getNpcsToInteract();
        for (final UUID npc : npcsToInteract) {
            result.add(new ObjectiveData(ObjectiveType.TALK_TO_NPC, stageInfo +
                    BukkitLang.get("talkTo"), 1, npc.toString()));
        }
        final List<UUID> npcsToKill = stage.getNpcsToKill();
        final List<Integer> npcKillCounts = stage.getNpcNumToKill();
        for (int i = 0; i < npcsToKill.size() && i < npcKillCounts.size(); i++) {
            final int goal = npcKillCounts.get(i);
            result.add(new ObjectiveData(ObjectiveType.KILL_NPC, stageInfo +
                    BukkitLang.get("kill").replace("<mob>", "NPC")
                    .replace(": <count>", ""), goal, npcsToKill.get(i).toString()));
        }
        final List<EntityType> tameMobs = stage.getMobsToTame();
        final List<Integer> tameCounts = stage.getMobNumToTame();
        for (int i = 0; i < tameMobs.size() && i < tameCounts.size(); i++) {
            final int goal = tameCounts.get(i);
            result.add(new ObjectiveData(ObjectiveType.TAME_MOB, stageInfo +
                    BukkitLang.get("tame")
                    .replace(": <count>", ""), goal, tameMobs.get(i).toString()));
        }
        final Integer fishToCatch = stage.getFishToCatch();
        if (fishToCatch != null && fishToCatch > 0) {
            result.add(new ObjectiveData(ObjectiveType.CATCH_FISH, stageInfo +
                    BukkitLang.get("catchFish")
                    .replace(": <count>", ""), fishToCatch, "fish"));
        }
        final Integer cowsToMilk = stage.getCowsToMilk();
        if (cowsToMilk != null && cowsToMilk > 0) {
            result.add(new ObjectiveData(ObjectiveType.MILK_COW, stageInfo +
                    BukkitLang.get("milkCow")
                    .replace(": <count>", ""), cowsToMilk, "cow"));
        }
        final Integer playersToKill = stage.getPlayersToKill();
        if (playersToKill != null && playersToKill > 0) {
            result.add(new ObjectiveData(ObjectiveType.KILL_PLAYER, stageInfo +
                    BukkitLang.get("killPlayer")
                    .replace(": <count>", ""), playersToKill, "player"));
        }
        final List<DyeColor> sheepToShear = stage.getSheepToShear();
        final List<Integer> shearCounts = stage.getSheepNumToShear();
        for (int i = 0; i < sheepToShear.size() && i < shearCounts.size(); i++) {
            final int goal = shearCounts.get(i);
            result.add(new ObjectiveData(ObjectiveType.SHEAR_SHEEP, stageInfo +
                    BukkitLang.get("shearSheep")
                    .replace("<count>", ""), goal, sheepToShear.get(i).toString()));
        }
        final List<String> locations = stage.getLocationNames();
        for (final String loc : locations) {
            result.add(new ObjectiveData(ObjectiveType.REACH_LOCATION, stageInfo +
                    BukkitLang.get("goTo").replace("<location>", loc), 1, loc));
        }
        final List<String> passwords = stage.getPasswordDisplays();
        for (final String pw : passwords) {
            result.add(new ObjectiveData(ObjectiveType.PASSWORD, stageInfo +
                    BukkitLang.get("stageEditorPassword"), 1, pw));
        }
        return result;
    }

    private static class ObjectiveData {
        final ObjectiveType type;
        final String description;
        final int goal;
        final String identifier;

        ObjectiveData(final ObjectiveType type, final String description, final int goal, final String identifier) {
            this.type = type;
            this.description = description;
            this.goal = goal;
            this.identifier = identifier;
        }
    }
}
