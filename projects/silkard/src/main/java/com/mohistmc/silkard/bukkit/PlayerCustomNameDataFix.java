package com.mohistmc.silkard.bukkit;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;
import net.minecraft.util.datafix.fixes.References;

/**
 * CraftBukkit - clears the legacy string CustomName of players before EntityCustomNameToComponentFix runs.
 *
 * @author Mgazul
 * @date 2026/11/1
 */
public class PlayerCustomNameDataFix extends DataFix {

    public PlayerCustomNameDataFix(Schema schema) {
        super(schema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("Player CustomName", this.getInputSchema().getType(References.PLAYER), typed ->
                typed.update(DSL.remainderFinder(), dynamic -> {
                    String s = dynamic.get("CustomName").asString("");
                    return s.isEmpty()
                            ? dynamic.remove("CustomName")
                            : dynamic.set("CustomName", LegacyComponentDataFixUtils.createPlainTextComponent(dynamic.getOps(), s));
                }));
    }
}
