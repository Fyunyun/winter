// package com.winter.modules.battle.core;

// import com.winter.modules.battle.model.BattleEffectConfig;
// import com.winter.modules.battle.model.BattleGroup;
// import com.winter.modules.battle.model.BattleResult;
// import com.winter.modules.battle.model.BattleUnit;
// import com.winter.modules.battle.model.EffectType;
// import com.winter.modules.battle.model.SoldiersType;
// import com.winter.modules.battle.model.skill.GeneralSkill;
// import com.winter.modules.battle.model.skill.SkillTrigger;

// import java.util.List;

// public class BattleDemo {
//     public static void main(String[] args) {
//         BattleGroup attacker = new BattleGroup("attacker");
//         BattleGroup defender = new BattleGroup("defender");

//         BattleUnit atkHero = buildUnit(101, SoldiersType.INFANTRY, 1500, 220, 60);
//         BattleUnit defHero = buildUnit(201, SoldiersType.CAVALRY, 1600, 200, 70);

//         atkHero.addSkill(new GeneralSkill(
//                 1001,
//                 SkillTrigger.BEFORE_ATTACK,
//                 0.35,
//                 1,
//                 null,
//                 List.of(new BattleEffectConfig(EffectType.MOD_DAMAGE, 50, 0))
//         ));
//         atkHero.addSkill(new GeneralSkill(
//                 1002,
//                 SkillTrigger.AFTER_ATTACK,
//                 1.0,
//                 0,
//                 null,
//                 List.of(new BattleEffectConfig(EffectType.VAMPIRE, 20, 0))
//         ));
//         defHero.addSkill(new GeneralSkill(
//                 2001,
//                 SkillTrigger.ON_DAMAGED,
//                 0.25,
//                 1,
//                 null,
//                 List.of(new BattleEffectConfig(EffectType.HEAL, 120, 0))
//         ));

//         attacker.addUnit(atkHero);
//         attacker.addUnit(buildUnit(102, SoldiersType.ARCHER, 900, 150, 40));

//         defender.addUnit(defHero);
//         defender.addUnit(buildUnit(202, SoldiersType.INFANTRY, 1000, 145, 45));

//         long seed = 20260214L;
//         BattleEngine engine = new BattleEngine(attacker, defender, seed);
//         BattleResult result = engine.simulate();

//         System.out.println("=== Battle Demo ===");
//         System.out.println("seed=" + result.getSeed() + ", attackerWin=" + result.isWin());
//         for (String log : result.getBattleLogs()) {
//             System.out.println(log);
//         }
//     }

//     private static BattleUnit buildUnit(int id, SoldiersType type, int hp, int atk, int def) {
//         BattleUnit unit = new BattleUnit();
//         unit.setId(id);
//         unit.setType(type);
//         unit.setMaxHp(hp);
//         unit.setHp(hp);
//         unit.setBaseAtk(atk);
//         unit.setBaseDef(def);
//         return unit;
//     }
// }
