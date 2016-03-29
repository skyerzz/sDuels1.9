package com.sky.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

public class Battle implements Listener{

	Inventories inv;
	Kits Ckits;
	
	Main main;
	public Battle(Main main)
	{
		this.main = main;
		inv = new Inventories(this.main);
		Ckits = new Kits(this.main);
		loadStrings(main.yml);
		loadOthers(main.yml);
		main.getServer().getPluginManager().registerEvents(this, main);
	}
	
	public void loadStrings(YamlConfiguration yml)
	{	
		String temp = yml.getString("duelcancelledmessage");
		if(temp!=null)
		{
			this.cancelled = temp.replace("&", "§");
		}
		
		temp = yml.getString("noarenas");
		if(temp!=null)
		{
			this.noarena = temp.replace("&", "§");
		}
		
		temp = yml.getString("startbattle");
		if(temp!=null)
		{
			this.startbattle = temp.replace("&", "§");
		}
		
		temp = yml.getString("countdownmessage");
		if(temp!=null)
		{
			this.countdownmessage = temp.replace("&", "§");
		}
		
		temp = yml.getString("readymessage");
		if(temp!=null)
		{
			this.readyMessage = temp.replace("&", "§");
		}
		
		temp = yml.getString("scoreboardname");
		if(temp!=null)
		{
			this.scoreboardName = temp.replace("&", "§");
		}
		
		temp = yml.getString("bottomscoreboard");
		if(temp!=null)
		{
			this.bottomscoreboard = temp.replace("&", "§");
		}
		
		temp = yml.getString("winmessage");
		if(temp!=null)
		{
			this.winmessage = temp.replace("&", "§");
		}
		
		temp = yml.getString("lossmessage");
		if(temp!=null)
		{
			this.lossmessage = temp.replace("&", "§");
		}
	}
	
	public void loadOthers(YamlConfiguration yml)
	{
		String temp = yml.getString("afterbattlelocation");
		if(temp==null)
		{
			main.getLogger().severe("afterbattlelocation is not correct in the config!");
			return;
		}
		try
		{
			String[] sLoc = temp.split(",");
			int x = Integer.parseInt(sLoc[0]);
			int y = Integer.parseInt(sLoc[1]);
			int z = Integer.parseInt(sLoc[2]);
			float yaw = new Float(sLoc[3]);
			float pitch = new Float(sLoc[4]);
			String worldname = sLoc[5];
			this.endbattle = new Location(Bukkit.getWorld(worldname), x, y, z ,yaw ,pitch);
		}
		catch(NumberFormatException | ArrayIndexOutOfBoundsException e)
		{
			main.getLogger().severe("afterbattlelocation is not correct in the config!");
		}
	}
	
	public String cancelled = "§cYour opponent gave up, Duel is cancelled!";
	public String noarena = "§cNo available arenas, Duel is cancelled!";
	public String startbattle = "§6GO!";
	public String countdownmessage = "§6The match begins in <seconds> seconds!";
	public String readyMessage = "§6Your opponent is ready!";
	public String winmessage = "§6You Won!";
	public String lossmessage = "§6You Lost!";
	
	public String scoreboardName = "§4Duel";
	public String bottomscoreboard = "§4pvp.sandosity.com";
	
	public HashMap<Player, Player> duels = new HashMap<Player, Player>();
	public HashMap<Player, String> kits = new HashMap<Player, String>();
	public HashMap<Player, ItemStack[]> oldInventory = new HashMap<Player, ItemStack[]>();
	public HashMap<Player, ItemStack[]> oldArmor = new HashMap<Player, ItemStack[]>();
	public HashMap<Player, Location> arena = new HashMap<Player, Location>();
	public ArrayList<Player> ingame = new ArrayList<Player>();
	public ArrayList<Player> freeze = new ArrayList<Player>();
	public ArrayList<Player> choosing = new ArrayList<Player>();
	public Location endbattle = new Location(Bukkit.getWorld("world"), 0, 0, 0);
	
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event)
	{
		if(freeze.contains(event.getPlayer()))
		{
			event.setTo(event.getFrom());
		}
	}
	
	@EventHandler
	public void onTeleport(PlayerTeleportEvent event)
	{
		if(!this.duels.containsKey(event.getPlayer()) || this.freeze.contains(event.getPlayer()))
		{
			return;
		}
		double distance = event.getFrom().distance(event.getTo());
		if(distance < 10)
		{
			return;
		}
		Player winner = this.duels.get(event.getPlayer());
		this.win(winner);
	}
	
	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event)
	{
		if(this.duels.containsKey(event.getPlayer()))
		{
			//ensure the player gets teleported before the connection is closed, to not have them spawn there next time they log in.
			event.getPlayer().teleport(this.endbattle);
			//also make sure that their potion effects run out
			for(PotionEffect effect: event.getPlayer().getActivePotionEffects())
			{
				event.getPlayer().removePotionEffect(effect.getType());
			}
			Player winner = this.duels.get(event.getPlayer());
			this.win(winner);			
		}
	}
	
	@EventHandler
	public void onPlayerDeath(EntityDamageEvent event)
	{
		if(event.getEntity() instanceof Player)
		{
			Player player = (Player) event.getEntity();
			if(player.getHealth() - event.getFinalDamage() <= 0)
			{
				if(this.duels.containsKey(player))
				{
					event.setCancelled(true);
					Player winner = this.duels.get(player);
					winner.setHealth(20);
					player.setHealth(20);
					this.win(winner);			
				}
			}
		}
		
	}
	
	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event)
	{
		if(!(event.getPlayer() instanceof Player))
		{
			return;
		}
		Player player = (Player) event.getPlayer();
		if(this.choosing.contains(player))
		{
			Bukkit.getScheduler().runTaskLaterAsynchronously(this.main, new Runnable()
			{
				   @Override
				   public void run() 
				   {
					   //player.getOpenInventory somehow doesnt return null, but the normal players inventory if none are opened.
					   if(player.getOpenInventory().countSlots() == 41)
					   {
						   inv.showMenu(player, 1);
					   }
				   }
			}, 5L);
		}
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event)
	{
		if(!(event.getWhoClicked() instanceof Player))
		{
			return;
		}
		Player player = (Player) event.getWhoClicked();
		if(event.getClickedInventory()==null)
		{
			return;
		}
		if(event.getClickedInventory().getName()==null)
		{
			return;
		}
		if(!event.getClickedInventory().getName().toLowerCase().contains("kit menu"))
		{
			return;
		}
		event.setCancelled(true);
		if(!this.choosing.contains(player))
		{
			if(event.getSlot() == 45 || event.getSlot() == 53)
			{
				String name = event.getCurrentItem().getItemMeta().getDisplayName().toLowerCase();
				name = name.replace("page ", "").trim();
				int newpage = 1;
				try
				{
					newpage = Integer.parseInt(name);
				}
				catch(NumberFormatException e)
				{
				}
				this.inv.showMenu(player, newpage);
				return;
			}
			return;
		}
		if(event.getCurrentItem()==null)
		{
			return;
		}
		ItemStack clicked = event.getCurrentItem();		
		if(clicked.getItemMeta()==null)
		{
			return;
		}
		if(clicked.getItemMeta().getDisplayName()==null)
		{
			return;
		}
		
		//check if they clicked on a page, or a kit
		if(event.getSlot() == 45 || event.getSlot() == 53)
		{
			String name = event.getCurrentItem().getItemMeta().getDisplayName().toLowerCase();
			name = name.replace("page ", "").trim();
			int newpage = 1;
			try
			{
				newpage = Integer.parseInt(name);
			}
			catch(NumberFormatException e)
			{
			}
			this.inv.showMenu(player, newpage);
			return;
		}
		else if(event.getSlot() == 49)
		{
			this.giveUp(player, this.cancelled);
			return;
		}
		
		this.Ckits.givePlayerKit(player, clicked.getItemMeta().getDisplayName());
		this.kits.put(player, clicked.getItemMeta().getDisplayName());
		this.ready(player);
	}
	
	public void ready(Player player)
	{
		Player opponent = this.duels.get(player);
		opponent.sendMessage(this.readyMessage);
		this.choosing.remove(player);
		if(!this.choosing.contains(opponent))
		{
			this.countDown(player, opponent);
		}
		player.closeInventory();
	}
	
	public void startBattle(Player challenger, Player defender)
	{
		if(challenger==null || defender==null)
		{
			System.out.println("startbattle has a null player, cancelled!");
			return;
		}
		duels.put(challenger, defender);
		duels.put(defender, challenger);
		
		
		Location loc = this.getArena();
		if(loc==null)
		{
			//no available arenas are found, we cancel the match
			duels.remove(challenger);
			duels.remove(defender);
			challenger.sendMessage(noarena);
			defender.sendMessage(noarena);
			return;
		}
		Location loc2 = Main.arenas.get(loc);
		
		//save the inventories from the players, so they can get it back after the match.
		ItemStack[] cInv = challenger.getInventory().getContents();
		ItemStack[] dInv = defender.getInventory().getContents();
		this.oldInventory.put(challenger, cInv);
		this.oldInventory.put(defender, dInv);
		this.oldArmor.put(challenger, challenger.getInventory().getArmorContents());
		this.oldArmor.put(defender, defender.getInventory().getArmorContents());
		challenger.getInventory().clear();
		defender.getInventory().clear();
		
		//make sure that this arena cannot be used by other people
		Main.occupiedArenas.put(loc, Main.arenas.get(loc));
		Main.arenas.remove(loc);
		this.arena.put(challenger, loc);
		
		
		//make both players choose their kit, freeze them while they do.
		this.freeze.add(challenger);
		this.freeze.add(defender);
		choosing.add(challenger);
		choosing.add(defender);		

		//teleport the players to said arena
		challenger.teleport(loc);
		defender.teleport(loc2);
		
		
		inv.showMenu(challenger, 1);
		inv.showMenu(defender, 1);	
		
	}
	
	public void countDown(Player player, Player opponent)
	{		
		//give both players a scoreboard and their kit
		this.ingame.add(player);
		this.ingame.add(opponent);
		this.updateScoreBoard(player);
		this.updateScoreBoard(opponent);
		
		//countdown from 5.
		new BukkitRunnable() 
		{
			int i = 0;
			   @Override
			   public void run() 
			   {
				   if(i>=5)
				   {
					   //start the battle, unfreeze them and cancel the runnable.
					   player.sendMessage(startbattle);
					   opponent.sendMessage(startbattle);
					   freeze.remove(player);
					   freeze.remove(opponent);
					   //lets heal the players to begin a fair match
					   player.setHealth(20);
					   player.setSaturation(20);
					   player.setFoodLevel(20);
					   opponent.setHealth(20);
					   opponent.setSaturation(20);
					   opponent.setFoodLevel(20);
					   this.cancel();
					   return;
				   }
				   //send both players the countdown message
				   String count = 5-i + "";
				   String currentcountdownmessage = countdownmessage.replace("<seconds>", count);
				   player.sendMessage(currentcountdownmessage);
				   opponent.sendMessage(currentcountdownmessage);
				   i++;
			   }
		}.runTaskTimer(this.main, 0L, 20L);

	}
	
	public void win(Player player)
	{
		Player loser = this.duels.get(player);
		
		//give the players their old inventory back
		loser.getInventory().clear();
		loser.getInventory().setContents(this.oldInventory.get(loser));
		loser.getInventory().setArmorContents(this.oldArmor.get(loser));
		loser.updateInventory();
		player.getInventory().clear();
		player.getInventory().setContents(this.oldInventory.get(player));
		player.getInventory().setArmorContents(this.oldArmor.get(player));
		player.updateInventory();
		
		
		this.oldInventory.remove(player);
		this.oldInventory.remove(loser);
		this.oldArmor.remove(player);
		this.oldArmor.remove(loser);
		
		
		
		//remove effects from both players
		for(PotionEffect effect: player.getActivePotionEffects())
		{
			player.removePotionEffect(effect.getType());
		}
		for(PotionEffect effect: loser.getActivePotionEffects())
		{
			loser.removePotionEffect(effect.getType());
		}
		
	
		if(!this.ingame.contains(player))
		{
			//they were not ingame yet, which means the countdown hasnt started. if someone won now
			//its likely to be by a leave of the other player. Therefore we cancel the duel.
			giveUp(player, this.cancelled);
			return;
		}

		//little message

		loser.sendMessage(this.lossmessage);
		player.sendMessage(this.winmessage);
		
		//give the winner some stats
		PlayerData pPD = Main.getPlayerData(player);
		pPD.wins++;
		String kit = kits.get(player);
		if(pPD.kitwins.containsKey(kit))
		{
			pPD.kitwins.replace(kit, pPD.kitwins.get(kit));
		}
		else
		{
			pPD.kitwins.put(kit, 1);
		}
		pPD.addPreviousDuel(loser.getUniqueId().toString(), 1);
		pPD.addPreviousDuelWin(loser.getUniqueId().toString(), 1);
		if(!player.isOnline())
		{
			pPD.save();
		}
		
		//give the loser some stats
		PlayerData lPD = Main.getPlayerData(loser);
		lPD.losses++;
		kit = kits.get(loser);
		if(lPD.kitloss.containsKey(kit))
		{
			lPD.kitloss.replace(kit, lPD.kitloss.get(kit));
		}
		else
		{
			lPD.kitloss.put(kit, 1);
		}
		lPD.addPreviousDuel(player.getUniqueId().toString(), 1);
		if(!loser.isOnline())
		{
			lPD.save();
		}
		
		//remove both players from all data stored again and teleport them to specified location in config.
		this.duels.remove(player);
		this.duels.remove(loser);
		this.kits.remove(player);
		this.kits.remove(loser);
		this.ingame.remove(player);
		this.ingame.remove(loser);		
		
		player.teleport(this.endbattle);
		loser.teleport(this.endbattle);
		

		//free the arena again
		Location loc = this.arena.get(player);
		if(loc==null)
		{
			loc = this.arena.get(loser);
		}
		Location loc2 = Main.occupiedArenas.get(loc);
		Main.arenas.put(loc, loc2);
		Main.occupiedArenas.remove(loc);
		this.arena.remove(player);
		this.arena.remove(loser);
		
		//remove the scoreboard for both players
		if(player.isOnline())
		{
			player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
		}
		if(loser.isOnline())
		{
			loser.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
		}
	}
	
	public void updateScoreBoard(Player player)
	{
		Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
		if(board==null)
		{
			return;
		}
		if(scoreboardName==null)
		{
			scoreboardName = "§4NULL";
		}
		if(scoreboardName.length() > 16)
		{
			return;
		}
		final Objective o = board.registerNewObjective(scoreboardName, "dummy");
		
		
	    o.setDisplaySlot(DisplaySlot.SIDEBAR);	    
	    
	    //empty line
	    Score s1 = o.getScore("");
	    s1.setScore(4);
	    
	    //set player + chosen kit
	    String kitname = "NULL";
	    String kit = this.kits.get(player);
	    if(kit!=null)
	    {
	    	kitname = kit;
	    }
	    Score s2 = o.getScore(player.getName() + "§6: " + kitname);
	    s2.setScore(3);
	    
	    //get the kit from the opponent next.
	    Player opponent = this.duels.get(player);	    
	    String kitname2 = "NULL";
	    String kit2 = this.kits.get(opponent);
	    if(kit2!=null)
	    {
	    	kitname2 = kit2;
	    }
	    Score s3 = o.getScore(opponent.getName() + "§6: " + kitname2);
	    s3.setScore(2);	  
	    
	    Score s4 = o.getScore(" ");
	    s4.setScore(1);
	   
	    Score s5 = o.getScore(this.bottomscoreboard);
	    s5.setScore(0);
	   
	    
	    
		player.setScoreboard(board);
	}
	
	public void giveUp(Player player, String reason)
	{
		Player loser = player;
		player = this.duels.get(player);
		player.sendMessage(reason);
		loser.sendMessage(reason);
		this.duels.remove(player);
		this.duels.remove(loser);
		this.kits.remove(player);
		this.kits.remove(loser);
		this.choosing.remove(player);
		this.choosing.remove(loser);
		this.ingame.remove(player);
		this.ingame.remove(loser);
		this.freeze.remove(loser);
		this.freeze.remove(player);
		player.closeInventory();
		loser.closeInventory();
		
		//free the arena again
		Location loc = this.arena.get(player);
		if(loc==null)
		{
			loc = this.arena.get(loser);
		}
		Location loc2 = Main.occupiedArenas.get(loc);
		Main.arenas.put(loc, loc2);
		Main.occupiedArenas.remove(loc);
		this.arena.remove(player);
		this.arena.remove(loser);

		player.teleport(this.endbattle);
		loser.teleport(this.endbattle);
		return;
	}
	
	public Location getArena()
	{
		Random rand = new Random();
		if(Main.arenas.size()<1)
		{
			return null;
		}
		int randomNumb = rand.nextInt(Main.arenas.size());
		int i = 0;
		for(Location loc : Main.arenas.keySet())
		{
			if(i==randomNumb)
			{				
				return loc;
			}
		}
		return null;
	}
}
