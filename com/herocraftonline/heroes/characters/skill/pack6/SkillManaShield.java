//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.ardea.heroes.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillManaShield extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillManaShield(Heroes plugin) {
        super(plugin, "ManaShield");
        this.setDescription("Create a magical barrier around you for $1 seconds by using your mana as a shield. Incoming damage to your health pool is negated by $2%, but instead your mana pool is drained at a $3% rate. Running out of mana makes the shield drop early.");
        this.setUsage("/skill manashield");
        this.setArgumentRange(0, 0);
        this.setIdentifiers(new String[]{"skill manashield"});
        this.setTypes(new SkillType[]{SkillType.SILENCEABLE, SkillType.BUFFING, SkillType.ABILITY_PROPERTY_MAGICAL});
        Bukkit.getPluginManager().registerEvents(new SkillManaShield.SkillHeroListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        double duration = Util.formatDouble((double)SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false) / 1000.0D);
        double resistValue = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, "resist-value", 0.2D, false) * 100.0D);
        double manaConversionRate = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, "mana-per-damage", 0.8D, false) * 100.0D);
        return this.getDescription().replace("$1", duration + "").replace("$2", resistValue + "").replace("$3", manaConversionRate + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set("resist-value", 0.2D);
        node.set("mana-per-damage", 0.8D);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has created a mana shield!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has dropped their mana shield.");
        return node;
    }

    public void init() {
        super.init();
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has created a mana shield!").replace("%hero%", "$1");
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has dropped their mana shield.").replace("%hero%", "$1");
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        this.broadcastExecuteText(hero);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1., 0), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 0.5, 0), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0F, 2.0F);
        long duration = (long)SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        hero.addEffect(new SkillManaShield.ManaShieldEffect(this, player, duration));
        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {
        private final Skill skill;

        public SkillHeroListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(
                priority = EventPriority.HIGHEST,
                ignoreCancelled = true
        )
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.getEntity() instanceof Player) {
                Hero hero = SkillManaShield.this.plugin.getCharacterManager().getHero((Player)event.getEntity());
                if (hero.hasEffect("ManaShield")) {
                    double resistValue = 1.0D - SkillConfigManager.getUseSetting(hero, this.skill, "resist-value", 0.2D, false);
                    double newDamage = event.getDamage() * resistValue;
                    double manaConversionRate = SkillConfigManager.getUseSetting(hero, this.skill, "mana-per-damage", 0.8D, false);
                    int mana = (int)((event.getDamage() - newDamage) * manaConversionRate);
                    if (hero.getMana() > mana) {
                        hero.setMana(hero.getMana() - mana);
                        event.setDamage(newDamage);
                    } else {
                        hero.setMana(0);
                        event.setDamage(newDamage);
                        ExpirableEffect effect = (ExpirableEffect)hero.getEffect("ManaShield");
                        effect.expire();
                    }

                    hero.getPlayer().getWorld().spawnParticle(Particle.SWEEP_ATTACK, hero.getPlayer().getLocation().add(0, 1., 0), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    hero.getPlayer().getWorld().spawnParticle(Particle.SWEEP_ATTACK, hero.getPlayer().getLocation().add(0, 0.5, 0), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0F, 2.0F);
                }
            }

        }
    }

    public class ManaShieldEffect extends ExpirableEffect {
        public ManaShieldEffect(Skill skill, Player applier, long duration) {
            super(skill, "ManaShield", applier, duration, SkillManaShield.this.applyText, SkillManaShield.this.expireText);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.MAGIC);
            this.types.add(EffectType.DISPELLABLE);
        }
    }
}
