package com.hearthsim.card.basic.spell;

import com.hearthsim.card.spellcard.SpellTargetableCard;
import com.hearthsim.event.effect.EffectCharacter;
import com.hearthsim.event.effect.EffectCharacterHeal;

public class HolyLight extends SpellTargetableCard {

    private static final byte HEAL_AMOUNT = 6;

    /**
     * Constructor
     *
     * @param hasBeenUsed Whether the card has already been used or not
     */
    @Deprecated
    public HolyLight(boolean hasBeenUsed) {
        this();
        this.hasBeenUsed = hasBeenUsed;
    }

    /**
     * Constructor
     *
     * Defaults to hasBeenUsed = false
     */
    public HolyLight() {
        super();
    }

    /**
     *
     * Use the card on the given target
     *
     * Heal a character for 6
     *
     *
     *
     * @param side
     * @param boardState The BoardState before this card has performed its action.  It will be manipulated and returned.
     *
     * @return The boardState is manipulated and returned
     */
    @Override
    public EffectCharacter getTargetableEffect() {
        if (this.effect == null) {
            this.effect = new EffectCharacterHeal(HolyLight.HEAL_AMOUNT);
        }
        return this.effect;
    }
}
