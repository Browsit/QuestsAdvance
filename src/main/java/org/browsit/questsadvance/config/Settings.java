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

package org.browsit.questsadvance.config;

import org.browsit.questsadvance.QuestsAdvance;
import org.bukkit.configuration.file.FileConfiguration;

public class Settings {
    private final QuestsAdvance plugin;
    private String rootName;
    private String background;
    private boolean showToast;
    private boolean announceChat;

    public Settings(final QuestsAdvance plugin) {
        this.plugin = plugin;
    }

    public void init() {
        final FileConfiguration cfg = plugin.getConfig();
        if (!cfg.contains("advance")) {
            plugin.getLogger().warning("No 'advance' node found in config.yml");
        }
        rootName = cfg.getString("advance.root-name", "Quests");
        background = cfg.getString("advance.background", "bookshelf");
        showToast = cfg.getBoolean("advance.show-toast", true);
        announceChat = cfg.getBoolean("advance.announce-chat", false);
    }

    public String getRootName() {
        return rootName;
    }

    public String getBackground() {
        return background;
    }

    public boolean canShowToast() {
        return showToast;
    }

    public boolean canAnnounceChat() {
        return announceChat;
    }
}
