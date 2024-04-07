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

package org.browsit.questsadvance;

import com.fren_gor.ultimateAdvancementAPI.AdvancementMain;
import com.fren_gor.ultimateAdvancementAPI.UltimateAdvancementAPI;
import me.pikamug.quests.Quests;
import org.browsit.questsadvance.advancements.AdvanceManager;
import org.browsit.questsadvance.config.Settings;
import org.browsit.questsadvance.listeners.PlayerListener;
import org.browsit.questsadvance.listeners.QuestListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class QuestsAdvance extends JavaPlugin {
    private Quests quests;
    private AdvancementMain main;
    private UltimateAdvancementAPI api;
    private Settings settings;
    private AdvanceManager advancementManager;

    @Override
    public void onLoad() {
        main = new AdvancementMain(this);
        main.load();
    }

    @Override
    public void onEnable() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Quests") == null) {
            getLogger().severe("Quests was not enabled! Fix Quests to allow linkage.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        quests = (Quests) Bukkit.getServer().getPluginManager().getPlugin("Quests");

        getConfig().options().copyDefaults(true);
        getConfig().options().header("QuestsAdvance configuration");
        saveConfig();

        main.enableInMemory();
        api = UltimateAdvancementAPI.getInstance(this);
        settings = new Settings(this);
        settings.init();

        advancementManager = new AdvanceManager(this, quests, api);

        getServer().getPluginManager().registerEvents(new QuestListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    @Override
    public void onDisable() {
        advancementManager.unregisterQuestsTab();
        main.disable();
    }

    public AdvanceManager getAdvancementManager() {
        return advancementManager;
    }

    public Quests getQuests() {
        return quests;
    }

    public UltimateAdvancementAPI getAdvancementAPI() {
        return api;
    }

    public Settings getSettings() {
        return settings;
    }
}
