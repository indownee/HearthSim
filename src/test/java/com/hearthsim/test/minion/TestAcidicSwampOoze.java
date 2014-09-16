package com.hearthsim.test.minion;

import com.hearthsim.card.Card;
import com.hearthsim.card.Deck;
import com.hearthsim.card.minion.Minion;
import com.hearthsim.card.minion.concrete.AcidicSwampOoze;
import com.hearthsim.card.spellcard.concrete.TheCoin;
import com.hearthsim.card.weapon.concrete.FieryWarAxe;
import com.hearthsim.exception.HSException;
import com.hearthsim.model.BoardModel;
import com.hearthsim.util.tree.HearthTreeNode;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestAcidicSwampOoze {

	private HearthTreeNode board;
	private Deck deck;
	private static final byte mana = 2;
	private static final byte attack0 = 5;
	private static final byte health0 = 3;
	private static final byte health1 = 7;

	@Before
	public void setup() throws HSException {
		board = new HearthTreeNode(new BoardModel());

		Minion minion0_0 = new Minion("" + 0, mana, attack0, health0, attack0, health0, health0);
		Minion minion0_1 = new Minion("" + 0, mana, attack0, (byte)(health1 - 1), attack0, health1, health1);
		Minion minion1_0 = new Minion("" + 0, mana, attack0, health0, attack0, health0, health0);
		Minion minion1_1 = new Minion("" + 0, mana, attack0, (byte)(health1 - 1), attack0, health1, health1);
		
		board.data_.placeMinion(0, minion0_0);
		board.data_.placeMinion(0, minion0_1);
		
		board.data_.placeMinion(1, minion1_0);
		board.data_.placeMinion(1, minion1_1);
		
		Card cards[] = new Card[10];
		for (int index = 0; index < 10; ++index) {
			cards[index] = new TheCoin();
		}
	
		deck = new Deck(cards);

		AcidicSwampOoze fb = new AcidicSwampOoze();
		board.data_.placeCard_hand_p0(fb);

		board.data_.setMana_p0((byte)4);
		board.data_.setMana_p1((byte)4);
		
		board.data_.setMaxMana_p0((byte)4);
		board.data_.setMaxMana_p1((byte)4);
		
	}
	
	@Test
	public void test0() throws HSException {
		
		Minion target = board.data_.getCharacter(1, 0);
		Card theCard = board.data_.getCard_hand_p0(0);
		HearthTreeNode ret = theCard.useOn(1, target, board, deck, null);
		
		assertTrue(ret == null);
		assertEquals(board.data_.getNumCards_hand(), 1);
		assertEquals(board.data_.getNumMinions_p0(), 2);
		assertEquals(board.data_.getNumMinions_p1(), 2);
		assertEquals(board.data_.getHero_p0().getHealth(), 30);
		assertEquals(board.data_.getHero_p1().getHealth(), 30);
		assertEquals(board.data_.getMinion_p0(0).getHealth(), health0);
		assertEquals(board.data_.getMinion_p0(1).getHealth(), health1 - 1);
		assertEquals(board.data_.getMinion_p1(0).getHealth(), health0);
		assertEquals(board.data_.getMinion_p1(1).getHealth(), health1 - 1);
	}
		
	@Test
	public void test2() throws HSException {
		
		Minion target = board.data_.getCharacter(0, 2);
		Card theCard = board.data_.getCard_hand_p0(0);
		HearthTreeNode ret = theCard.useOn(0, target, board, deck, null);
		
		assertFalse(ret == null);
		assertEquals(board.data_.getNumCards_hand(), 0);
		assertEquals(board.data_.getNumMinions_p0(), 3);
		assertEquals(board.data_.getNumMinions_p1(), 2);
		assertEquals(board.data_.getHero_p0().getHealth(), 30);
		assertEquals(board.data_.getHero_p1().getHealth(), 30);
		assertEquals(board.data_.getMinion_p0(0).getHealth(), health0);
		assertEquals(board.data_.getMinion_p0(1).getHealth(), health1 - 1);
		assertEquals(board.data_.getMinion_p0(2).getHealth(), 2);
		assertEquals(board.data_.getMinion_p1(0).getHealth(), health0);
		assertEquals(board.data_.getMinion_p1(1).getHealth(), health1 - 1);
	}
	
	@Test
	public void test3() throws HSException {
		
		Minion target = board.data_.getCharacter(0, 0);
		HearthTreeNode fl = new HearthTreeNode(board.data_.flipPlayers());
		fl.data_.placeCard_hand_p0(new FieryWarAxe());
		fl = fl.data_.getCard_hand_p0(0).useOn(0, target, fl, deck, null);
		board = new HearthTreeNode(fl.data_.flipPlayers());
		
		assertEquals(board.data_.getHero_p0().getTotalAttack(), 0);
		assertEquals(board.data_.getHero_p0().getWeaponCharge(), 0);
		assertEquals(board.data_.getHero_p1().getTotalAttack(), 3);
		assertEquals(board.data_.getHero_p1().getWeaponCharge(), 2);

		target = board.data_.getCharacter(0, 2);
		Card theCard = board.data_.getCard_hand_p0(0);
		HearthTreeNode ret = theCard.useOn(0, target, board, deck, null);
		
		assertFalse(ret == null);
		assertEquals(board.data_.getNumCards_hand(), 0);
		assertEquals(board.data_.getNumMinions_p0(), 3);
		assertEquals(board.data_.getNumMinions_p1(), 2);
		assertEquals(board.data_.getHero_p0().getHealth(), 30);
		assertEquals(board.data_.getHero_p1().getHealth(), 30);

		assertEquals(board.data_.getHero_p0().getTotalAttack(), 0);
		assertEquals(board.data_.getHero_p0().getWeaponCharge(), 0);
		assertEquals(board.data_.getHero_p1().getTotalAttack(), 0);
		assertEquals(board.data_.getHero_p1().getWeaponCharge(), 0);

		assertEquals(board.data_.getMinion_p0(0).getHealth(), health0);
		assertEquals(board.data_.getMinion_p0(1).getHealth(), health1 - 1);
		assertEquals(board.data_.getMinion_p0(2).getHealth(), 2);
		assertEquals(board.data_.getMinion_p1(0).getHealth(), health0);
		assertEquals(board.data_.getMinion_p1(1).getHealth(), health1 - 1);
	}
}
