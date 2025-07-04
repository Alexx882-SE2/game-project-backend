package com.se2gruppe5.risikobackend.game.objects;

import com.se2gruppe5.risikobackend.common.objects.Player;
import com.se2gruppe5.risikobackend.common.objects.Territory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class GameUnitTest {
    private UUID gameId;
    private UUID player1Id;
    private UUID player2Id;
    private ConcurrentHashMap<UUID, Player> players;
    private ArrayList<Territory> customTerritories;
    private Game game;

    @BeforeEach
    void setup() {
        gameId = UUID.randomUUID();
        customTerritories = new ArrayList<>();
        //Initialize Players
        player1Id = UUID.randomUUID();
        player2Id = UUID.randomUUID();
        Player player1 = new Player(player1Id, "Markus", 0xFF0000);
        Player player2 = new Player(player2Id, "Leo", 0x00FF00);
        players = new ConcurrentHashMap<>();
        players.put(player1Id, player1);
        players.put(player2Id, player2);

        //Create Game
        game = new Game(gameId, players, customTerritories);
    }

    @Test
    void testConstructorAndStartInitializesCorrectly() {
        game.start();

        assertEquals(0, game.getPhaseIndex());
        assertFalse(game.getRequiresPlayerChange());

        assertTrue(game.getPlayerTurnOrder().getFirst().isCurrentTurn());
        assertFalse(game.getPlayerTurnOrder().getLast().isCurrentTurn());
    }

    // fixme split these tests into individual phases, where each test's result is a precondition for the next
    @Test
    void phaseOrderTest() {
        game.start();

        //Nach setup initial phase #1
        assertEquals(0, game.getPhaseIndex());
        assertFalse(game.getRequiresPlayerChange());

        //nextPhase -> phase #2
        game.nextPhase();
        assertEquals(1, game.getPhaseIndex());
        assertFalse(game.getRequiresPlayerChange());

        //nextPhase -> phase #3
        game.nextPhase();
        assertEquals(2, game.getPhaseIndex());
        assertFalse(game.getRequiresPlayerChange());

        //nextPhase -> wrap around back to phase #1 + playerchange required
        game.nextPhase();
        assertEquals(0, game.getPhaseIndex());
        assertTrue(game.getRequiresPlayerChange());
    }

    @Test
    void playerTurnOrderTest() {
        game.start();

        //Nach Setup p1 turn
        assertTrue(game.getPlayerTurnOrder().getFirst().isCurrentTurn());
        assertFalse(game.getPlayerTurnOrder().getLast().isCurrentTurn());
        game.nextPlayer();

        //NextPlayer -> p2 turn
        assertFalse(game.getPlayerTurnOrder().getFirst().isCurrentTurn());
        assertTrue(game.getPlayerTurnOrder().getLast().isCurrentTurn());

        //NextPlayer -> wraparound zurück zu p1
        game.nextPlayer();
        assertTrue(game.getPlayerTurnOrder().getFirst().isCurrentTurn());
        assertFalse(game.getPlayerTurnOrder().getLast().isCurrentTurn());
    }

    @Test
    void changeTerritoryTest() {
        Territory original = new Territory(UUID.randomUUID(), 1, 1);
        customTerritories.add(original);

        ArrayList<Territory> territories = game.getTerritories();
        Territory added = territories.getFirst();
        assertEquals(original, added);

        Territory updated = new Territory(original.owner(), original.stat() + 5, original.id());
        game.changeTerritory(updated);
        assertTrue(game.getTerritories().contains(updated));
        assertFalse(game.getTerritories().contains(original));

        //Attempt invalid changes
        assertThrows(IllegalArgumentException.class, () ->
                game.changeTerritory(new Territory(original.owner(), 10, -1))
        );

        assertThrows(IllegalArgumentException.class, () ->
                game.changeTerritory(new Territory(player1Id, 10, 99))
        );
    }

    @Test
    void updatePlayerTest() {
        customTerritories.add(new Territory(player1Id, 0, 1));
        int color = 0x0F0F0F;
        Player modified = new Player(player2Id, "Markus123", color);
        game.updatePlayer(modified);
        assertEquals(color, game.getPlayers().get(player2Id).getColor());

        //Attempt invalid assignments
        UUID newStranger = UUID.randomUUID();
        Player invalid = new Player(newStranger, "Stranger", 0x404404);
        assertThrows(IllegalArgumentException.class, () -> game.updatePlayer(invalid));
    }

    @Test
    void temporaryTerritoryDivideTest() { //todo must most likely be changed

        assertDoesNotThrow(() -> game.assignTerritories());
        game.getTerritories().forEach(t ->
                assertTrue(players.containsKey(t.owner()))
        );

        customTerritories.add(new Territory(player1Id, 1, 1));
        customTerritories.add(new Territory(player2Id, 2, 1));
        customTerritories.add(new Territory(player1Id, 3, 1));


        assertThrows(IllegalStateException.class, () -> game.assignTerritories());
    }


    @Test
    void startingTroopDistributionTest() {
        customTerritories.add(new Territory(UUID.randomUUID(), 11, 1));
        customTerritories.add(new Territory(UUID.randomUUID(), 22, 2));

        assertEquals(customTerritories, game.getTerritories());
        assertDoesNotThrow(() -> game.distributeStartingTroops(5));
        game.getTerritories().forEach(t -> assertTrue(t.stat() >= 1));
    }
}
