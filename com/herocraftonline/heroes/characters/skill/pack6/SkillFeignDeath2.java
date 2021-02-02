package com.ardea.heroes.skills;

import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.effects.common.InvisibleEffect;
import com.herocraftonline.heroes.characters.party.HeroParty;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class SkillFeignDeath extends PassiveSkill {
    private static final Random random = new Random();
    private String applyText;
    private String expireText;
    private SkillFeignDeath.FeignDeathMoveChecker moveChecker;


    public SkillFeignDeath(Heroes plugin) {
        super(plugin, "FeignDeath");
        this.setDescription("You have a $1% chance to evade death. Instead of dying, you gain $2% health when you would otherwise have died.");
        this.setTypes(new SkillType[]{SkillType.HEALING, SkillType.BUFFING, SkillType.ABILITY_PROPERTY_DARK, SkillType.STEALTHY, SkillType.ABILITY_PROPERTY_ILLUSION});
        this.setIdentifiers(new String[]{"skill feigndeath"});
        this.setNotes(new String[]{"Note: Taking damage, moving, or causing damage removes the effect"});
        this.moveChecker = new SkillFeignDeath.FeignDeathMoveChecker(this);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillFeignDeath.FeignDeathListener(this), plugin);
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this.moveChecker, 1L, 1L);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.CHANCE.node(), 0.5);
        node.set("health-percent-on-rebirth", 0.5D);
        node.set(SkillSetting.COOLDOWN.node(), 600000);
        node.set("randomness", 20);
        node.set(SkillSetting.DURATION.node(), 30000);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "You FeignDeath into the shadows");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "You come back into view");
        node.set("detection-range", 0);
        node.set("max-move-distance", 1.0D);
        return node;
    }

    public void init() {
        super.init();
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "You FeignDeath into the shadows");
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "You come back into view");
    }


    public String getDescription(Hero hero) {
        double health = SkillConfigManager.getUseSetting(hero, this, "health-percent-on-rebirth", 0.5D, false) * 100.0;
        double chance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE.node(), 0.5, false) * 100.0;
        int cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN.node(), 600000, false);
        return this.getDescription().replace("$1", chance + "").replace("$2", health + "").replace("$3", cooldown + "");
    }

    public class FeignDeathListener implements Listener {
        private final Skill skill;
        private String applyText;
        private String expireText;
        private SkillFeignDeath.FeignDeathMoveChecker moveChecker;

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
                            player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 3);
                            broadcast(hero.getPlayer().getLocation(), ChatColor.GRAY + "[" + ChatColor.DARK_RED + "Skill" + ChatColor.GRAY + "] " + ChatColor.DARK_RED + hero.getName() + ChatColor.GRAY + " has risen from the ashes like a " + ChatColor.WHITE + "FeignDeath" + ChatColor.GRAY + "!");
                            //teleport part below
                            Location loc1 = player.getLocation();
                            int randomness = SkillConfigManager.getUseSetting(hero, this.skill, "randomness", 20, false);
                            int x = loc1.getBlockX() + random.nextInt(randomness);
                            int z = loc1.getBlockZ() + random.nextInt(randomness);
                            Double highestBlock = (double)player.getWorld().getHighestBlockYAt(loc1) + 1;
                            loc1.setY(highestBlock);
                            loc1.setX(x);
                            loc1.setZ(z);
                            player.teleport(loc1);
                            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.5F, 1.0F);
                            //invisibility part below
                            long duration = (long)SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.DURATION, 30000, false);
                            hero.addEffect(new InvisibleEffect(this.skill, player, duration, this.applyText, this.expireText));
                            this.moveChecker.addHero(hero);
                        }
                    }
                }
            }
        }
    }

    public class FeignDeathMoveChecker implements Runnable {
        private Map<Hero, Location> oldLocations = new HashMap();
        private Skill skill;

        FeignDeathMoveChecker(Skill skill) {
            this.skill = skill;
        }

        public void run() {
            Iterator heroes = this.oldLocations.entrySet().iterator();

            while(true) {
                while(heroes.hasNext()) {
                    Map.Entry<Hero, Location> entry = (Map.Entry)heroes.next();
                    Hero hero = (Hero)entry.getKey();
                    Location oldLoc = (Location)entry.getValue();
                    if (!hero.hasEffect("Invisible")) {
                        heroes.remove();
                    } else {
                        Location newLoc = hero.getPlayer().getLocation();
                        if (newLoc.getWorld() == oldLoc.getWorld() && newLoc.distance(oldLoc) <= SkillConfigManager.getUseSetting(hero, this.skill, "max-move-distance", 1.0D, false)) {
                            if (newLoc.getBlock().getLightLevel() > SkillConfigManager.getUseSetting(hero, this.skill, "max-light-level", 8, false)) {
                                hero.removeEffect(hero.getEffect("Invisible"));
                                heroes.remove();
                            } else {
                                double detectRange = SkillConfigManager.getUseSetting(hero, this.skill, "detection-range", 0.0D, false);
                                Iterator var8 = hero.getPlayer().getNearbyEntities(detectRange, detectRange, detectRange).iterator();

                                while(var8.hasNext()) {
                                    Entity entity = (Entity)var8.next();
                                    if (entity instanceof Player) {
                                        if (hero.hasParty()) {
                                            Hero nearHero = SkillFeignDeath.this.plugin.getCharacterManager().getHero((Player)entity);
                                            HeroParty heroParty = hero.getParty();
                                            boolean isPartyMember = false;
                                            Iterator var13 = heroParty.getMembers().iterator();

                                            while(var13.hasNext()) {
                                                Hero partyMember = (Hero)var13.next();
                                                if (nearHero.equals(partyMember)) {
                                                    isPartyMember = true;
                                                    break;
                                                }
                                            }

                                            if (isPartyMember) {
                                                return;
                                            }
                                        }

                                        hero.removeEffect(hero.getEffect("Invisible"));
                                        heroes.remove();
                                        break;
                                    }
                                }
                            }
                        } else {
                            hero.removeEffect(hero.getEffect("Invisible"));
                            heroes.remove();
                        }
                    }
                }

                return;
            }
        }

        public void addHero(Hero hero) {
            if (hero.hasEffect("Invisible")) {
                this.oldLocations.put(hero, hero.getPlayer().getLocation());
            }
        }
    }

}