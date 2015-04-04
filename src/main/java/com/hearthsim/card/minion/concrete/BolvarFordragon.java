package com.hearthsim.card.minion.concrete;

import com.hearthsim.card.minion.Minion;
import com.hearthsim.card.minion.MinionDamagedInterface;
import com.hearthsim.card.minion.MinionDeadInterface;
import com.hearthsim.event.CharacterFilter;
import com.hearthsim.event.effect.CardEffectCharacter;
import com.hearthsim.event.effect.CardEffectCharacterBuff;
import com.hearthsim.event.effect.CardEffectCharacterBuffDelta;
import com.hearthsim.event.effect.CardEffectCharacterDraw;
import com.hearthsim.model.PlayerSide;
import com.hearthsim.util.tree.HearthTreeNode;

public class BolvarFordragon extends Minion implements MinionDeadInterface {

    private final static CharacterFilter filter = CharacterFilter.FRIENDLY_MINIONS;

    public BolvarFordragon() {
        super();
    }

    @Override
    public HearthTreeNode minionDeadEvent(PlayerSide thisMinionPlayerSide, PlayerSide deadMinionPlayerSide, Minion deadMinion, HearthTreeNode boardState) {
        if (!this.isInHand()) {
            return boardState;
        }

        if (!BolvarFordragon.filter.targetMatches(thisMinionPlayerSide, this, deadMinionPlayerSide, deadMinion, boardState.data_)) {
            return boardState;
        }

        // TODO would like to reuse action pattern here but right now that is locked to targeting things on board
        this.addAttack((byte) 1);
        return boardState;
    }
}
