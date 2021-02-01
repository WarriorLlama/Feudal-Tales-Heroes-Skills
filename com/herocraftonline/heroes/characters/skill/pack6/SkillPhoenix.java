package com.ardea.heroes.skills;

import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.CharacterDamageEvent;
import com.herocraftonline.heroes.characters.Hero;

public class SkillPhoenix extends PassiveSkill
    //edit line above?
{
    public static SkillPhoenix skill;

    public SkillPhoenix(Heroes plugin) {
        super(plugin,"Phoenix");
        this.setDescription("You have a $1% chance to gain full health when you would have died.");
        this.setTypes(new SkillType[]{SkillType.HEALING,SkillType.BUFFING});
        this.setIdentifiers(new String[]{"skill phoenix"});
        Bukkit.getServer().getPluginManager().registerEvents(new SkillPhoenix.PhoenixListener(this), plugin);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.CHANCE.node(), 0.5);
        return node;
    }

    public String getDescription(Hero hero) {
        double chance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE.node(), 0.5, false) * 100.0;
        return this.getDescription().replace("$1", chance + "");
    }
    
    public class PhoenixListener implements Listener {
        private final Skill skill;

        public PhoenixListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler
        public void onCharacterDamageEvent(CharacterDamageEvent event) {
            if(!(event.getEntity() instanceof Player))
                return;
            Hero hero = plugin.getCharacterManager().getHero((Player)event.getEntity());
            if(!hero.hasAccessToSkill(skill))
                return;
            if((((Damageable)hero.getPlayer()).getHealth() - event.getDamage()) > 0)
                return;
            double chance = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.CHANCE, 0.5, false);
            if(Math.random() > chance)
                return;
            hero.getPlayer().setHealth(((Damageable)hero.getPlayer()).getMaxHealth());
            broadcast(hero.getPlayer().getLocation(), ChatColor.GRAY + "[" + ChatColor.DARK_RED + "Skill" + ChatColor.GRAY + "] " + ChatColor.DARK_RED + hero.getName() + ChatColor.GRAY +
                    " has risen from the ashes like a " + ChatColor.WHITE + "Phoenix" + ChatColor.GRAY + "!");
        }
    }
}