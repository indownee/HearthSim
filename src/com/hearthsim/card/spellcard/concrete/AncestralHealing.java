package com.hearthsim.card.spellcard.concrete;

import com.hearthsim.card.Deck;
import com.hearthsim.card.minion.Minion;
import com.hearthsim.card.spellcard.SpellCard;
import com.hearthsim.exception.HSInvalidPlayerIndexException;
import com.hearthsim.util.tree.HearthTreeNode;

public class AncestralHealing extends SpellCard {

	/**
	 * Constructor
	 * 
	 * @param hasBeenUsed Whether the card has already been used or not
	 */
	public AncestralHealing(boolean hasBeenUsed) {
		super("Ancestral Healing", (byte)0, hasBeenUsed);
	}

	/**
	 * Constructor
	 * 
	 * Defaults to hasBeenUsed = false
	 */
	public AncestralHealing() {
		this(false);
	}

	@Override
	public Object deepCopy() {
		return new AncestralHealing(this.hasBeenUsed_);
	}
	
	/**
	 * 
	 * Use the card on the given target
	 * 
	 * This card heals the target minion up to full health and gives it taunt.  Cannot be used on heroes.
	 * 
	 * @param thisCardIndex The index (position) of the card in the hand
	 * @param playerIndex The index of the target player.  0 if targeting yourself or your own minions, 1 if targeting the enemy
	 * @param minionIndex The index of the target minion.
	 * @param boardState The BoardState before this card has performed its action.  It will be manipulated and returned.
	 * 
	 * @return The boardState is manipulated and returned
	 */
	@Override
	protected HearthTreeNode use_core(
			int thisCardIndex,
			int playerIndex,
			int minionIndex,
			HearthTreeNode boardState,
			Deck deck)
		throws HSInvalidPlayerIndexException
	{
		if (minionIndex == 0) {
			//cant't use it on the heroes
			return null;
		}
		
		Minion targetMinion;
		if (playerIndex == 0) {
			targetMinion = boardState.data_.getMinion_p0(minionIndex-1);			
		} else {
			targetMinion = boardState.data_.getMinion_p1(minionIndex-1);
		}
		if (targetMinion.getHealth() < targetMinion.getMaxHealth()) 
			targetMinion.takeHeal((byte)(targetMinion.getMaxHealth() - targetMinion.getHealth()), playerIndex, minionIndex, boardState, deck);
		targetMinion.setTaunt(true);
		return super.use_core(thisCardIndex, playerIndex, minionIndex, boardState, deck);
	}
}
