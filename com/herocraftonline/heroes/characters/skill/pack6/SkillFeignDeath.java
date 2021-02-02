package com.ardea.heroes.skills;

import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.effects.common.InvisibleEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Random;

public class SkillFeignDeath extends PassiveSkill {
    private static final Random random = new Random();

    public SkillFeignDeath(Heroes plugin) {
        super(plugin, "FeignDeath");
        this.setDescription("You have a $1% chance to evade death. Instead of dying, you gain $2% health when you would otherwise have died.");
        this.setTypes(new SkillType[]{SkillType.HEALING, SkillType.BUFFING, SkillType.ABILITY_PROPERTY_DARK, SkillType.STEALTHY, SkillType.ABILITY_PROPERTY_ILLUSION});
        this.setIdentifiers(new String[]{"skill feigndeath"});
        this.setNotes(new String[]{"Note: Taking damage, moving, or causing damage removes the effect"});
        Bukkit.getServer().getPluginManager().registerEvents(new SkillFeignDeath.FeignDeathListener(this), plugin);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.CHANCE.node(), 0.5);
        node.set("health-percent-on-rebirth", 0.5D);
        node.set(SkillSetting.COOLDOWN.node(), 600000);
        node.set("randomness", 20);
        node.set(SkillSetting.DURATION.node(), 30000);
        node.set("detection-range", 0);
        node.set("max-move-distance", 1.0D);
        return node;
    }

    public String getDescription(Hero hero) {
        double health = SkillConfigManager.getUseSetting(hero, this, "health-percent-on-rebirth", 0.5D, false) * 100.0;
        double chance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE.node(), 0.5, false) * 100.0;
        int cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN.node(), 600000, false);
        return this.getDescription().replace("$1", chance + "").replace("$2", health + "").replace("$3", cooldown + "");
    }

    public class FeignDeathListener implements Listener {
        private final Skill skill;

        public FeignDeathListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(
                priority = EventPriority.MONITOR
        )
        public void onEntityDamage(EntityDamageEvent event) {
            if (!event.isCancelled() && event.getEntity() instanceof Player && event.getDamage() != 0.0D) {
                Player player = (Player) event.getEntity();
                Hero hero = SkillFeignDeath.this.plugin.getCharacterManager().getHero(player);
                double currentHealth = player.getHealth();
                if (currentHealth <= event.getDamage()) {
                    if (hero.hasEffect("FeignDeath") && (hero.getCooldown("FeignDeath") == null || hero.getCooldown("FeignDeath") <= System.currentTimeMillis())) {
                        double chance = SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.CHANCE.node(), 0.5, false);
                        if (Math.random() <= chance) {
                            double regainPercent = SkillConfigManager.getUseSetting(hero, this.skill, "health-percent-on-rebirth", 0.5D, false);
                            double healthRegain = player.getMaxHealth() * regainPercent;
                            HeroRegainHealthEvent hrh = new HeroRegainHealthEvent(hero, healthRegain, this.skill, hero);
                            if (hrh.isCancelled() || (Double) hrh.getDelta() == 0.0D) {
                                return;
                            }
                            event.setDamage(0.0D);
                            event.setCancelled(true);
                            hero.heal((Double) hrh.getDelta());
                            long cooldown = (long) (SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.COOLDOWN.node(), 600000, false));
                            hero.setCooldown("FeignDeath", cooldown + System.currentTimeMillis());
                            broadcast(hero.getPlayer().getLocation(), ChatColor.GRAY + "[" + ChatColor.DARK_RED + "Skill" + ChatColor.GRAY + "] " + ChatColor.DARK_RED + hero.getName() + ChatColor.GRAY + " has risen from the ashes like a " + ChatColor.WHITE + "FeignDeath" + ChatColor.GRAY + "!");
                            //teleport part below
                            Location loc1 = player.getLocation();
                            int randomness = SkillConfigManager.getUseSetting(hero, this.skill, "randomness", 20, false);
                            int x = loc1.getBlockX() + random.nextInt(randomness);
                            int z = loc1.getBlockZ() + random.nextInt(randomness);
                            Double highestBlock = (double)player.getWorld().getHighestBlockYAt(loc1) + 2;
                            loc1.setY(highestBlock);
                            loc1.setX(x);
                            loc1.setZ(z);
                            player.teleport(loc1);
                            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.5F, 1.0F);
                            //invisibility part below
                            long duration = (long)SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.DURATION, 30000, false);
                            hero.addEffect(new InvisibleEffect(this.skill, player, duration));
                        }
                    }
                }
            }
        }
    }
}