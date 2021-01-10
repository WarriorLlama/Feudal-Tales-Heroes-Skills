//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.herocraftonline.heroes.characters.skill.pack6;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import java.text.DecimalFormat;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillLevitate extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillLevitate(Heroes plugin) {
        super(plugin, "Levitate");
        this.setDescription("You levitate in the air for $1 seconds. Flight duration is increased with Intellect.");
        this.setUsage("/skill levitate");
        this.setArgumentRange(0, 0);
        this.setIdentifiers(new String[]{"skill levitate"});
        this.setTypes(new SkillType[]{SkillType.SILENCEABLE, SkillType.BUFFING, SkillType.MOVEMENT_INCREASING});
    }
    // all good above this point ADD NODE LINE BELOW?

    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 1500, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_INTELLECT, 30, false);
        duration += hero.getAttributeValue(AttributeType.INTELLECT) * durationIncrease;
        String formattedDuration = Util.decFormat.format((double)duration / 1000.0D);
        DecimalFormat dF = new DecimalFormat("##.#");
        return this.getDescription().replace("$1", dF.format((long)SkillConfigManager.getUseSetting(hero, this, "levitate-duration", 1000, false) / 1000L) + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 1500);
        node.set(SkillSetting.DURATION_INCREASE_PER_INTELLECT.node(), 30);
        return node;
    }

    public void init() {
        super.init();
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% begins to levitate!").replace("%hero%", "$1");
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is no longer levitating!").replace("%hero%", "$1");
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        this.broadcastExecuteText(hero);
        //levitate duration below
        int levitateDuration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 1500, false);
        int levitateDurationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_INTELLECT, 30, false);
        levitateDuration += hero.getAttributeValue(AttributeType.INTELLECT) * levitateDurationIncrease;
        //fall duration below
        int fallDuration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 1500, false);
        int fallDurationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_INTELLECT, 30, false);
        fallDuration += hero.getAttributeValue(AttributeType.INTELLECT) * fallDurationIncrease;
        //add levitate and fall values below
        hero.addEffect(new LevitateEffect(this, "Levitate", hero.getPlayer(), ((levitateDuration * 10) / 12), 0, this.applyText, ""));
        hero.addEffect(new FallEffect(this, "Fall", hero.getPlayer(), fallDuration, 1, "", this.expireText));
        //create particle effects below
        player.getWorld().spawnParticle(Particle.SPELL, player.getLocation().add(0.0D, 0.5D, 0.0D), 50, 0.0D, 0.0D, 0.0D, 1.0D);
        //create immediate sound effect below
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, 1.1F, 0.8F);
        //create a delayed sound effect below, it only plays one time, need to use different code for multiple times
        Bukkit.getScheduler().runTaskLater(this.plugin, new Runnable() {
            public void run() {
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, 1.1F, 0.8F);
            }
        }, (long)fallDuration/100);
        //turn sound off after a delay below
        Bukkit.getScheduler().runTaskLater(this.plugin, new Runnable() {
            public void run() {
                player.stopSound(Sound.ITEM_ELYTRA_FLYING);
            }
        }, (long)fallDuration/50);
        //end of skill output
        return SkillResult.NORMAL;
    }

    public class LevitateEffect extends ExpirableEffect {
        public LevitateEffect(Skill skill, String name, Player applier, long duration, int amplifier, String applyText, String expireText) {
            super(skill, name, applier, duration, applyText, expireText);
            this.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, (int)(20L * duration / 1000L), amplifier), false);
        }
    }

    public class FallEffect extends ExpirableEffect {
        public FallEffect(Skill skill, String name, Player applier, long duration, int amplifier, String applyText, String expireText) {
            super(skill, name, applier, duration, applyText, expireText);
            this.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, (int)(20L * duration / 1000L), amplifier), false);
        }
    }


}
