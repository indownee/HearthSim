package com.hearthsim.card;

import com.hearthsim.card.minion.Hero;
import com.hearthsim.card.minion.Minion;
import com.hearthsim.card.spellcard.SpellRandomInterface;
import com.hearthsim.event.CharacterFilter;
import com.hearthsim.event.deathrattle.DeathrattleAction;
import com.hearthsim.event.effect.CardEffectAoeInterface;
import com.hearthsim.event.effect.CardEffectCharacter;
import com.hearthsim.event.effect.CardEffectRandomTargetInterface;
import com.hearthsim.event.effect.CardEffectTargetableInterface;
import com.hearthsim.exception.HSException;
import com.hearthsim.model.BoardModel;
import com.hearthsim.model.PlayerModel;
import com.hearthsim.model.PlayerSide;
import com.hearthsim.util.DeepCopyable;
import com.hearthsim.util.HearthAction;
import com.hearthsim.util.HearthAction.Verb;
import com.hearthsim.util.factory.BoardStateFactoryBase;
import com.hearthsim.util.tree.HearthTreeNode;
import com.hearthsim.util.tree.RandomEffectNode;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

public class Card implements DeepCopyable<Card> {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Name of the card
     */
    protected String name_ = "";

    /**
     * Mana cost of the card
     */
    protected byte baseManaCost;

    protected boolean hasBeenUsed;
    protected boolean isInHand_;

    /**
     * Overload handling
     */
    private byte overload;

    protected DeathrattleAction deathrattleAction_;

    /**
     * Constructor
     *
     * @param name Name of the card
     * @param baseManaCost Base mana cost of the card
     * @param hasBeenUsed Has the card been used?
     * @param isInHand Is the card in your hand?
     */
    public Card() {
        ImplementedCardList cardList = ImplementedCardList.getInstance();
        ImplementedCardList.ImplementedCard implementedCard = cardList.getCardForClass(this.getClass());
        this.initFromImplementedCard(implementedCard);
    }

    protected void initFromImplementedCard(ImplementedCardList.ImplementedCard implementedCard) {
        if (implementedCard != null) {
            this.name_ = implementedCard.name_;
            this.baseManaCost = (byte) implementedCard.mana_;
            this.overload = (byte) implementedCard.overload;
        }
        this.hasBeenUsed = false;
        this.isInHand_ = true;
    }

    /**
     * Get the name of the card
     *
     * @return Name of the card
     */
    public String getName() {
        return name_;
    }

    /**
     * Set the name of the card
     */
    public void setName(String value) {
        name_ = value;
    }

    /**
     * Get the mana cost of the card
     *
     * @param side The PlayerSide of the card for which you want the mana cost
     * @param board The BoardModel representing the current board state
     *
     * @return Mana cost of the card
     */
    public byte getManaCost(PlayerSide side, BoardModel board) {
        return baseManaCost;
    }

    /**
     * Set the mana cost of the card
     *
     * @param mana The new mana cost
     */
    public void setBaseManaCost(byte mana) {
        this.baseManaCost = mana;
    }

    /**
     * Get the base mana cost of the card
     *
     * @return Mana cost of the card
     */
    public byte getBaseManaCost() {
        return baseManaCost;
    }


    /**
     * Returns whether the card has been used or not
     *
     * @return
     */
    public boolean hasBeenUsed() {
        return hasBeenUsed;
    }

    /**
     * Sets whether the card has been used or not
     *
     * @param value The new hasBeenUsed value
     */
    public void hasBeenUsed(boolean value) {
        hasBeenUsed = value;
    }

    public void isInHand(boolean value) {
        isInHand_ = value;
    }

    protected boolean isInHand() {
        return isInHand_;
    }

    // This deepCopy pattern is required because we use the class of each card to recreate it under certain circumstances
    @Override
    public Card deepCopy() {
        Card copy = null;
        try {
            copy = getClass().newInstance();
        } catch(InstantiationException e) {
            log.error("instantiation error", e);
        } catch(IllegalAccessException e) {
            log.error("illegal access error", e);
        }
        if (copy == null) {
            throw new RuntimeException("unable to instantiate card.");
        }

        copy.name_ = this.name_;
        copy.baseManaCost = this.baseManaCost;
        copy.hasBeenUsed = this.hasBeenUsed;
        copy.isInHand_ = this.isInHand_;
        copy.overload = this.overload;

        return copy;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }

        if (this.getClass() != other.getClass()) {
            return false;
        }

        // More logic here to be discuss below...
        if (baseManaCost != ((Card)other).baseManaCost)
            return false;

        if (hasBeenUsed != ((Card)other).hasBeenUsed)
            return false;

        if (isInHand_ != ((Card)other).isInHand_)
            return false;

        if (!name_.equals(((Card)other).name_))
            return false;

        if (overload != ((Card)other).overload)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name_ != null ? name_.hashCode() : 0;
        result = 31 * result + baseManaCost;
        result = 31 * result + overload;
        result = 31 * result + (hasBeenUsed ? 1 : 0);
        result = 31 * result + (isInHand_ ? 1 : 0);
        return result;
    }

    /**
     * Returns whether this card can be used on the given target or not
     *
     * This function is an optional optimization feature. Some cards in Hearthstone have limited targets; Shadow Bolt cannot be used on heroes, Mind Blast can only target enemy heroes, etc. Even in this situation though, BoardStateFactory
     * will still try to play non- sensical moves because it doesn't know that such moves are invalid until it tries to play them. The problem is, the BoardStateFactory has to go and make a deep copy of the current BoardState before it can
     * go and try to play the invalid move, and it turns out that 98% of execution time in HearthSim is BoardStateFactory calling BoardState.deepCopy(). By overriding this function and returning false on appropriate occasions, we can save
     * some calls to deepCopy and get better performance out of the code.
     *
     * By default, this function only checks mana cost.
     *
     * @param playerSide
     * @param boardModel
     * @return
     */
    public boolean canBeUsedOn(PlayerSide playerSide, Minion minion, BoardModel boardModel) {
        if (!(this.getManaCost(PlayerSide.CURRENT_PLAYER, boardModel) <= boardModel.getCurrentPlayer().getMana())) {
            return false;
        }

        if (this instanceof CardEffectTargetableInterface && !((CardEffectTargetableInterface)this).getTargetableFilter().targetMatches(PlayerSide.CURRENT_PLAYER, this, playerSide, minion, boardModel)) {
            return false;
        }

        return true;
    }

    public boolean canBeUsedOn(PlayerSide playerSide, int targetIndex, BoardModel boardModel) {
        Minion targetMinion = boardModel.modelForSide(playerSide).getCharacter(targetIndex);
        return this.canBeUsedOn(playerSide, targetMinion, boardModel);
    }

    public final HearthTreeNode useOn(PlayerSide side, Minion targetMinion, HearthTreeNode boardState) throws HSException {
        return this.useOn(side, targetMinion, boardState, false);
    }

    public HearthTreeNode useOn(PlayerSide side, int targetIndex, HearthTreeNode boardState) throws HSException {
        return this.useOn(side, targetIndex, boardState, false);
    }

    public HearthTreeNode useOn(PlayerSide side, int targetIndex, HearthTreeNode boardState, boolean singleRealizationOnly) throws HSException {
        Minion target = boardState.data_.modelForSide(side).getCharacter(targetIndex);
        return this.useOn(side, target, boardState, singleRealizationOnly);
    }

    /**
     * Use the card on the given target
     *
     * @param side
     * @param targetMinion The target minion (can be a Hero)
     * @param boardState The BoardState before this card has performed its action. It will be manipulated and returned.
     * @param singleRealizationOnly For cards with random effects, setting this to true will return only a single realization of the random event.
     *
     * @return The boardState is manipulated and returned
     */
    private HearthTreeNode useOn(PlayerSide side, Minion targetMinion, HearthTreeNode boardState, boolean singleRealizationOnly) throws HSException {
        if (!this.canBeUsedOn(side, targetMinion, boardState.data_))
            return null;

        PlayerModel currentPlayer = boardState.data_.getCurrentPlayer();
        PlayerModel targetPlayer = boardState.data_.modelForSide(side);

        // Need to record card and target index *before* the board state changes
        int cardIndex = currentPlayer.getHand().indexOf(this);
        int targetIndex = targetMinion instanceof Hero ? 0 : targetPlayer.getMinions()
                .indexOf(targetMinion) + 1;

        currentPlayer.addNumCardsUsed((byte)1);

        HearthTreeNode toRet = this.notifyCardPlayBegin(boardState, singleRealizationOnly);
        if (toRet != null) {
            toRet = this.use_core(side, targetMinion, toRet, singleRealizationOnly);
            if (toRet != null && this.triggersOverload())
                toRet.data_.modelForSide(PlayerSide.CURRENT_PLAYER).addOverload(this.getOverload());
        }

        if (toRet != null) {
            toRet = this.notifyCardPlayResolve(toRet, singleRealizationOnly);
        }

        if (toRet != null) {
            toRet.setAction(new HearthAction(Verb.USE_CARD, PlayerSide.CURRENT_PLAYER, cardIndex, side, targetIndex));
        }

        return toRet;
    }

    /**
     * Use the card on the given target
     * <p>
     * This is the core implementation of card's ability
     *
     * @param side
     * @param boardState The BoardState before this card has performed its action. It will be manipulated and returned.
     * @return The boardState is manipulated and returned
     */
    protected HearthTreeNode use_core(
        PlayerSide side,
        Minion targetMinion,
        HearthTreeNode boardState,
        boolean singleRealizationOnly)
        throws HSException {
        HearthTreeNode toRet = boardState;

        CardEffectCharacter effect = null;
        if (this instanceof CardEffectTargetableInterface) {
            effect = ((CardEffectTargetableInterface)this).getTargetableEffect();
        }

        // TODO this is to workaround using super.use_core since we no longer have an accurate reference to the origin card (specifically, Soulfire messes things up)
        byte manaCost = this.getManaCost(PlayerSide.CURRENT_PLAYER, boardState.data_);

        // Check to see if this card generates RNG children
        if (this instanceof SpellRandomInterface) {
            int originIndex = boardState.data_.modelForSide(PlayerSide.CURRENT_PLAYER).getHand().indexOf(this);
            int targetIndex = boardState.data_.modelForSide(side).getIndexForCharacter(targetMinion);

            // create an RNG "base" that is untouched. This allows us to recreate the RNG children during history traversal.
            toRet = new RandomEffectNode(toRet, new HearthAction(HearthAction.Verb.USE_CARD, side, 0, side, 0));
            Collection<HearthTreeNode> children = ((SpellRandomInterface)this).createChildren(PlayerSide.CURRENT_PLAYER, originIndex, toRet);

            // for each child, apply the effect and mana cost. we want to do as much as we can with the non-random effect portion (e.g., the damage part of Soulfire)
            for (HearthTreeNode child : children) {
                if (effect != null) {
                    child = effect.applyEffect(PlayerSide.CURRENT_PLAYER, null, side, targetIndex, child);
                }
                child.data_.modelForSide(PlayerSide.CURRENT_PLAYER).subtractMana(manaCost);
                toRet.addChild(child);
            }
        } else {
            boolean removeCard = true;

            // if we have an effect, we need to apply it
            if (this instanceof CardEffectAoeInterface) {
                toRet = this.effectAllUsingFilter(((CardEffectAoeInterface) this).getAoeEffect(), ((CardEffectAoeInterface) this).getAoeFilter(), toRet);
            } else if (this instanceof CardEffectRandomTargetInterface) {
                removeCard = false;
                CardEffectRandomTargetInterface that = (CardEffectRandomTargetInterface)this;

                // create an RNG "base" that is untouched. This allows us to recreate the RNG children during history traversal.
                toRet = new RandomEffectNode(toRet, new HearthAction(HearthAction.Verb.USE_CARD, side, 0, side, 0));
                Collection<HearthTreeNode> children = this.effectRandomCharacterUsingFilter(that.getRandomTargetEffect(), that.getRandomTargetFilter(), toRet);

                // for each child, apply the effect and mana cost. we want to do as much as we can with the non-random effect portion (e.g., the damage part of Soulfire)
                for (HearthTreeNode child : children) {
                    child.data_.modelForSide(PlayerSide.CURRENT_PLAYER).subtractMana(manaCost);
                    this.notifyCardPlayResolve(child, singleRealizationOnly);
                    toRet.addChild(child);
                }
            } else if (effect != null) {
                toRet = effect.applyEffect(PlayerSide.CURRENT_PLAYER, this, side, targetMinion, toRet);
            }

            // apply standard card played effects
            if (toRet != null) {
                PlayerModel currentPlayer = toRet.data_.getCurrentPlayer();
                currentPlayer.subtractMana(manaCost);
                if (removeCard) {
                    currentPlayer.getHand().remove(this);
                }
            }
        }

        return toRet;
    }

    // ======================================================================================
    // Various notifications
    // ======================================================================================
    private HearthTreeNode notifyCardPlayBegin(HearthTreeNode boardState, boolean singleRealizationOnly) {
        PlayerModel currentPlayer = boardState.data_.getCurrentPlayer();
        PlayerModel waitingPlayer = boardState.data_.getWaitingPlayer();

        HearthTreeNode toRet = boardState;
        ArrayList<CardPlayBeginInterface> matches = new ArrayList<>();

        for (Card card : currentPlayer.getHand()) {
            if (card instanceof CardPlayBeginInterface) {
                matches.add((CardPlayBeginInterface)card);
            }
        }

        Card hero = currentPlayer.getHero();
        if (hero instanceof CardPlayBeginInterface) {
            matches.add((CardPlayBeginInterface)hero);
        }

        for (Minion minion : currentPlayer.getMinions()) {
            if (!minion.isSilenced() && minion instanceof CardPlayBeginInterface) {
                matches.add((CardPlayBeginInterface)minion);
            }
        }

        for (CardPlayBeginInterface match : matches) {
            toRet = match.onCardPlayBegin(PlayerSide.CURRENT_PLAYER, PlayerSide.CURRENT_PLAYER, this, toRet, singleRealizationOnly);
        }
        matches.clear();

        for (Card card : waitingPlayer.getHand()) {
            if (card instanceof CardPlayBeginInterface) {
                matches.add((CardPlayBeginInterface)card);
            }
        }

        hero = waitingPlayer.getHero();
        if (hero instanceof CardPlayBeginInterface) {
            matches.add((CardPlayBeginInterface)hero);
        }

        for (Minion minion : waitingPlayer.getMinions()) {
            if (!minion.isSilenced() && minion instanceof CardPlayBeginInterface) {
                matches.add((CardPlayBeginInterface)minion);
            }
        }

        for (CardPlayBeginInterface match : matches) {
            toRet = match.onCardPlayBegin(PlayerSide.WAITING_PLAYER, PlayerSide.CURRENT_PLAYER, this, toRet, singleRealizationOnly);
        }

        // check for and remove dead minions
        toRet = BoardStateFactoryBase.handleDeadMinions(toRet, singleRealizationOnly);
        return toRet;
    }

    private HearthTreeNode notifyCardPlayResolve(HearthTreeNode boardState, boolean singleRealizationOnly) {
        PlayerModel currentPlayer = boardState.data_.getCurrentPlayer();
        PlayerModel waitingPlayer = boardState.data_.getWaitingPlayer();

        HearthTreeNode toRet = boardState;
        ArrayList<CardPlayAfterInterface> matches = new ArrayList<>();

        for (Card card : currentPlayer.getHand()) {
            if (card instanceof CardPlayAfterInterface) {
                matches.add((CardPlayAfterInterface)card);
            }
        }

        Card hero = currentPlayer.getHero();
        if (hero instanceof CardPlayAfterInterface) {
            matches.add((CardPlayAfterInterface)hero);
        }

        for (Minion minion : currentPlayer.getMinions()) {
            if (!minion.isSilenced() && minion instanceof CardPlayAfterInterface) {
                matches.add((CardPlayAfterInterface)minion);
            }
        }

        for (CardPlayAfterInterface match : matches) {
            toRet = match.onCardPlayResolve(PlayerSide.CURRENT_PLAYER, PlayerSide.CURRENT_PLAYER, this, toRet, singleRealizationOnly);
        }
        matches.clear();

        for (Card card : waitingPlayer.getHand()) {
            if (card instanceof CardPlayAfterInterface) {
                matches.add((CardPlayAfterInterface)card);
            }
        }

        hero = waitingPlayer.getHero();
        if (hero instanceof CardPlayAfterInterface) {
            matches.add((CardPlayAfterInterface)hero);
        }

        for (Minion minion : waitingPlayer.getMinions()) {
            if (!minion.isSilenced() && minion instanceof CardPlayAfterInterface) {
                matches.add((CardPlayAfterInterface)minion);
            }
        }

        for (CardPlayAfterInterface match : matches) {
            toRet = match.onCardPlayResolve(PlayerSide.WAITING_PLAYER, PlayerSide.CURRENT_PLAYER, this, toRet, singleRealizationOnly);
        }

        // check for and remove dead minions
        toRet = BoardStateFactoryBase.handleDeadMinions(toRet, singleRealizationOnly);
        return toRet;
    }

    protected HearthTreeNode effectAllUsingFilter(CardEffectCharacter effect, CharacterFilter filter, HearthTreeNode boardState) {
        if (boardState != null && filter != null) {
            for (BoardModel.CharacterLocation location : boardState.data_) {
                Minion character = boardState.data_.getCharacter(location);
                if (filter.targetMatches(PlayerSide.CURRENT_PLAYER, this, location.getPlayerSide(), character, boardState.data_)) {
                    boardState = effect.applyEffect(PlayerSide.CURRENT_PLAYER, this, location.getPlayerSide(), character, boardState);
                }
            }
        }
        return boardState;
    }

    protected Collection<HearthTreeNode> effectRandomCharacterUsingFilter(CardEffectCharacter effect, CharacterFilter filter, HearthTreeNode boardState) {
        int originIndex = boardState.data_.modelForSide(PlayerSide.CURRENT_PLAYER).getHand().indexOf(this);
        ArrayList<HearthTreeNode> children = new ArrayList<>();
        for (BoardModel.CharacterLocation location : boardState.data_) {
            if (filter.targetMatches(PlayerSide.CURRENT_PLAYER, this, location.getPlayerSide(), location.getIndex(), boardState.data_)) {
                HearthTreeNode newState = new HearthTreeNode(boardState.data_.deepCopy());
                Card origin = newState.data_.getCharacter(PlayerSide.CURRENT_PLAYER, originIndex);
                effect.applyEffect(PlayerSide.CURRENT_PLAYER, origin, location.getPlayerSide(), location.getIndex(), newState);
                newState.data_.modelForSide(PlayerSide.CURRENT_PLAYER).getHand().remove(originIndex);
                children.add(newState);
            }
        }
        return children;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", name_);
        json.put("mana", baseManaCost);
        if (hasBeenUsed) json.put("hasBeenUsed", hasBeenUsed);
        return json;
    }

    @Override
    public String toString() {
        return this.toJSON().toString();
    }

    protected boolean isWaitingPlayer(PlayerSide side) {
        return PlayerSide.WAITING_PLAYER == side;
    }

    @Deprecated
    protected boolean isNotHero(Minion targetMinion) {
        return !isHero(targetMinion);
    }

    protected boolean isCurrentPlayer(PlayerSide side) {
        return PlayerSide.CURRENT_PLAYER == side;
    }

    protected byte getOverload() {
        return overload;
    }

    public void setOverload(byte value) {
        overload = value;
    }

    public boolean triggersOverload() {
        return overload > 0;
    }

    public boolean hasDeathrattle() {
        return deathrattleAction_ != null;
    }

    public DeathrattleAction getDeathrattle() {
        return deathrattleAction_;
    }

    public void setDeathrattle(DeathrattleAction action) {
        deathrattleAction_ = action;
    }


    @Deprecated
    public Card(String name, byte baseManaCost, boolean hasBeenUsed, boolean isInHand, byte overload) {
        this.baseManaCost = baseManaCost;
        this.hasBeenUsed = hasBeenUsed;
        isInHand_ = isInHand;
        name_ = name;
        this.overload = overload;
    }

    @Deprecated
    public Card(byte baseManaCost, boolean hasBeenUsed, boolean isInHand) {
        ImplementedCardList cardList = ImplementedCardList.getInstance();
        ImplementedCardList.ImplementedCard implementedCard = cardList.getCardForClass(this.getClass());
        name_ = implementedCard.name_;
        this.baseManaCost = baseManaCost;
        this.hasBeenUsed = hasBeenUsed;
        isInHand_ = isInHand;
        this.overload = (byte) implementedCard.overload;
    }

    @Deprecated
    public final HearthTreeNode useOn(PlayerSide side, Minion targetMinion, HearthTreeNode boardState,
                                      Deck deckPlayer0, Deck deckPlayer1) throws HSException {
        return this.useOn(side, targetMinion, boardState, false);
    }

    @Deprecated
    public HearthTreeNode useOn(PlayerSide side, int targetIndex, HearthTreeNode boardState, Deck deckPlayer0,
                                Deck deckPlayer1) throws HSException {
        return this.useOn(side, targetIndex, boardState, false);
    }

    @Deprecated
    protected boolean isHero(Minion targetMinion) {
        return targetMinion instanceof Hero;
    }

}
