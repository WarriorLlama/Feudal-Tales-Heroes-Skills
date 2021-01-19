//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillPoisonGas extends ActiveSkill {
    private String expireText;

    public SkillPoisonGas(Heroes plugin) {
        super(plugin, "PoisonGas");
        this.setDescription("You poison surrounding targets within $0 blocks. Monsters immune to poison take $1 damage over $2 seconds. More vulnerable targets take $3 damage over $2 seconds.");
        this.setUsage("/skill poisongas");
        this.setArgumentRange(0, 0);
        this.setIdentifiers(new String[]{"skill poisongas"});
        this.setTypes(new SkillType[]{SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.DEBUFFING, SkillType.AGGRESSIVE});
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.RADIUS.node(), 10);
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.DURATION_INCREASE_PER_INTELLECT.node(), 30);
        node.set(SkillSetting.PERIOD.node(), 1250);
        node.set("tick-damage", 1);
        node.set("max-targets", 5);
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has recovered from the poison!");
        return node;
    }

    public void init() {
        super.init();
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% has recovered from the poison!").replace("%target%", "$1");
    }

    public ArrayList<Location> circle(Location centerPoint, int particleAmount, double circleRadius) {
        World world = centerPoint.getWorld();
        double increment = 6.283185307179586D / (double)particleAmount;
        ArrayList<Location> locations = new ArrayList();

        for(int i = 0; i < particleAmount; ++i) {
            double angle = (double)i * increment;
            double x = centerPoint.getX() + circleRadius * Math.cos(angle);
            double z = centerPoint.getZ() + circleRadius * Math.sin(angle);
            locations.add(new Location(world, x, centerPoint.getY(), z));
        }
        return locations;
    }

//all should be fine above here unless adding max targets, or adding public array list
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        this.broadcastExecuteText(hero);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        long durationIncrease = (int)SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_INTELLECT, 30, false);
        duration += (long)hero.getAttributeValue(AttributeType.INTELLECT) * durationIncrease;
        long period = (long)SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1250, true);
        double tickDamage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 10, false);
        int maxTargets = SkillConfigManager.getUseSetting(hero, this, "max-targets", 0, false);
        int targetsHit = 0;
        List<Entity> entities = hero.getPlayer().getNearbyEntities((double)radius, (double)radius, (double)radius);
        Iterator var12 = entities.iterator();

        while(var12.hasNext()) {
            Entity entity = (Entity)var12.next();
            if (maxTargets > 0 && targetsHit >= maxTargets) {
                break;
            }
            if (entity instanceof LivingEntity) {
                LivingEntity target = (LivingEntity)entity;
                //the line below ensures players in creative mode cannot deal damage
                if (damageCheck(player, target)) {
                    this.addSpellTarget(target, hero);
                    CharacterTemplate targetCT = this.plugin.getCharacterManager().getCharacter(target);
                    SkillPoisonGas.PoisonSkillEffect pEffect = new SkillPoisonGas.PoisonSkillEffect(this, period, duration, tickDamage, player);
                    //weirdly, applying pEffect here intead of above in SkillResult section allows multiple mobs to take tick damage instead of just 1, but causes poison damage to be 2 dmg lower than it should be
                    targetCT.addEffect(pEffect);
                    ++targetsHit;
                }
            }
        }

        for(double r = 1.0D; r < (double)(radius * 2); ++r) {
            ArrayList<Location> particleLocations = this.circle(player.getLocation(), 200, r / 2.0D);

            for(int i = 0; i < particleLocations.size(); ++i) {
                player.getWorld().spawnParticle(Particle.SNEEZE, (Location)particleLocations.get(i), 1, 0.3D, 0.8D, 0.3D, 0.0D);
            }
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CREEPER_HURT, 8.0F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_BURN, 8.0F, 0.0F);
        return SkillResult.NORMAL;
    }

    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_INTELLECT, 30, false);
        duration += hero.getAttributeValue(AttributeType.INTELLECT) * durationIncrease;
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 10, false);
        double period = (double)SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1250, false);
        double damage = (double)SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
        return this.getDescription().replace("$0", radius + "").replace("$1", damage * (double)duration / period + "").replace("$2", duration / 1000 + "").replace("$3", damage * (double)duration / period * 4.0D + "");
    }

    public class PoisonSkillEffect extends PeriodicDamageEffect {
        public PoisonSkillEffect(Skill skill, long period, long duration, double tickDamage, Player applier) {
            super(skill, "Poison", applier, period, duration, tickDamage);
            this.types.add(EffectType.POISON);
            this.addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int)(20L * duration / 1000L), 0), true);
        }

        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
        }

        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            this.broadcast(player.getLocation(), SkillPoisonGas.this.expireText, new Object[]{player.getName()});
        }
    }
}
