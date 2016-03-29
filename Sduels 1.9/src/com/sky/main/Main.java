package com.sky.main;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class Main extends JavaPlugin implements Listener{

	Battle battle;
	
	public YamlConfiguration yml;
	
	public HashMap<Player, Player> duels = new HashMap<Player, Player>();
	public static HashMap<Player, PlayerData> playerData = new HashMap<Player, PlayerData>();
	public static HashMap<Location, Location> arenas = new HashMap<Location, Location>();
	public static HashMap<Location, Location> occupiedArenas = new HashMap<Location, Location>();
	
	public String duelInviteMessage = "§e<player> Has challenged you to a duel. Click to accept!";
	public String alreadyInGame = "§eOne of the players is already in a duel!";
	public String duelhovermessage = "§eClick to accept the duel!";
	public String noperms = "§cYou do not have permissions for this!";
	public String battleself = "§cYou cannot battle yourself!";
	public String nochallenge = "§cThis player did not challenge you!";
	public String duelconfirmation = "§eYou challanged <player> for a duel!";
	
	public String[] helpmessage;
	public static String maindatafolder;
	
	
	@Override
	public void onEnable() 
	{
		getLogger().info("Enabling Sduels");
		File path = new File(getDataFolder() + "/players");
	    if(!path.exists()){
	    	path.mkdirs();
	    }
	    for(Player player: Bukkit.getOnlinePlayers())
	    {
	    	Main.playerData.put(player, Main.getPlayerData(player));
	    }
	    Main.maindatafolder = this.getDataFolder() + "";
		this.getServer().getPluginManager().registerEvents(this, this);
		
		String path2 = this.getDataFolder() + "/config.yml";	    
		File file = new File(path2);
	    if (!file.exists()) {
	        getLogger().info("config.yml not found, creating!");
	        saveDefaultConfig();
	    } else {
	        getLogger().info("config.yml found, loading!");
	    }	    
		this.yml = YamlConfiguration.loadConfiguration(file);
		
		
	    this.battle = new Battle(this);
	    reload();
	    
	}
	
	@Override	
	public void onDisable() 
	{
		getLogger().info("Disabling Sduels");
		for(Player player: Main.playerData.keySet())
		{
			PlayerData PD = Main.playerData.get(player);
			PD.save();
		}
	}
	
	public void reload()
	{
		String path = this.getDataFolder() + "/config.yml";	    
		File file = new File(path);
	    if (!file.exists()) {
	        getLogger().info("config.yml not found, creating!");
	        saveDefaultConfig();
	    } else {
	        getLogger().info("config.yml found, loading!");
	    }
		this.yml = YamlConfiguration.loadConfiguration(file);
		
		loadArenas();		
		loadMessages();
		battle.loadOthers(this.yml);
		battle.loadStrings(this.yml);
		battle.Ckits.loadKits(this.yml);
	}
	
	public void loadArenas()
	{
		ConfigurationSection cs = yml.getConfigurationSection("arenas");
		if(cs==null)
		{
			Bukkit.getLogger().warning("No arenas are found in the config!");
			return;
		}
		for(String string: cs.getKeys(false))
		{
			Location loc1 = null, loc2 = null;
			for(int i = 1; i < 3; i++)
			{
				String[] location = cs.getString(string + ".location" + i).split(",");
				if(location == null)
				{
					break;
				}
				try
				{
					int x = Integer.parseInt(location[0]);
					int y = Integer.parseInt(location[1]);
					int z = Integer.parseInt(location[2]);
					float yaw = new Float(location[3]);
					float pitch = new Float(location[4]);
					if(i==1)
					{
						loc1 = new Location(Bukkit.getWorld(location[5]), x, y, z, yaw, pitch);
					}
					else
					{
						loc2 = new Location(Bukkit.getWorld(location[5]), x, y, z, yaw, pitch);
					}
				}
				catch(NumberFormatException | ArrayIndexOutOfBoundsException e)
				{
					Bukkit.getLogger().severe("Strings for the arenas configuration are not filled out correctly. Error on " + string + ".location" + i);
					continue;
				}
				
			}
			if(loc1==null || loc2==null)
			{
				continue;
			}
			arenas.put(loc1, loc2);
		}
	}

	@SuppressWarnings("unchecked")
	public void loadMessages()
	{
		List<String> helpmessage = (List<String>) yml.getList("helpmessage");
		this.helpmessage = new String[helpmessage.size()];
		int i = 0;
		for(String string: helpmessage)
		{
			this.helpmessage[i] = string.replace("&", "§");
			i++;
		}
		
		String temp = yml.getString("duelinvitemessage");
		if(temp!=null)
		{
			this.duelInviteMessage = temp.replace("&", "§");
		}
		
		temp = yml.getString("alreadyingamemessage");
		if(temp!=null)
		{
			this.alreadyInGame = temp.replace("&", "§");
		}
		
		temp = yml.getString("nopermissionmessage");
		if(temp!=null)
		{
			this.noperms = temp.replace("&", "§");
		}
		
		temp = yml.getString("battleself");
		if(temp!=null)
		{
			this.battleself = temp.replace("&", "§");
		}
		
		temp = yml.getString("nochallenge");
		if(temp!=null)
		{
			this.nochallenge = temp.replace("&", "§");
		}
		
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event){
		Player player = event.getPlayer();
		PlayerData PD = Main.getPlayerData(player);
		Main.playerData.put(player, PD);	    
	}
	
	@EventHandler
	public void onLogout(PlayerQuitEvent event)
	{
		Player player = event.getPlayer();		
		this.savePlayerData(player);
	}
	
	@EventHandler
	public void onKick(PlayerKickEvent event)
	{
		Player player = event.getPlayer();		
		this.savePlayerData(player);
	}
	
	public static PlayerData getPlayerData(OfflinePlayer objective)
	{
		if(objective == null)
		{
			System.out.println("PlayerData returning for player = null");
			String nullpath = Main.maindatafolder + "/players/null.yml";
			return new PlayerData(nullpath, null);
		}
		
		if(playerData.containsKey(objective))
		{
			return playerData.get(objective);
		}
		
		String path = Main.maindatafolder + "/players/" + objective.getUniqueId() + ".yml";
		System.out.println("PlayerData retrieved for player " + objective.getName());

		return new PlayerData(path, objective);
		
	}
	
	public void savePlayerData(OfflinePlayer player)
	{
		PlayerData PD = playerData.get(player);
		
		if(PD.path.equals(this.getDataFolder() + "/players/null.yml"))
		{
			System.out.println("PlayerData for " + player.getName() + " is null!");
			return;
		}
		
		this.getLogger().info("PlayerData saved on logout for player " + player.getName());
		PD.save();
		Main.playerData.remove(player);
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		  if(cmd.getName().equalsIgnoreCase("duel"))
		  {
			  if(!(sender instanceof Player))
			  {				  
				  return true;
			  }
			  Player player = (Player) sender;
			  Player p2 = null;
			  if(!player.hasPermission("duel.duel"))
			  {
				  player.sendMessage(this.noperms);
				  return true;
			  }
			  if(args.length == 0)
			  {
				  sendHelpMessage(player);
				  return true;
			  }
			  
			  if(args[0].equalsIgnoreCase("help"))
			  {
				  this.battle.inv.showMenu(player, 1);
				  sendHelpMessage(player);
				  return true;
			  }
			  else if(args[0].equalsIgnoreCase("version"))
			  {
				  player.sendMessage("§6running sDuels version " + this.getDescription().getVersion());
				  player.sendMessage("§6Author: skyerzz");
				  return true;
			  }
			  else if(args[0].equalsIgnoreCase("reload"))
			  {
				  if(!player.hasPermission("duel.reload"))
				  {
					  player.sendMessage(this.noperms);
					  return true;
				  }
				  this.reload();
				  player.sendMessage("Reloaded Sduel!");
				  return true;
			  }
			  
			  else if(args[0].equalsIgnoreCase("stats"))
			  {
				  if(!player.hasPermission("duel.viewstats"))
				  {
					  player.sendMessage(this.noperms);
					  return true;
				  }
				  if(args.length < 2)
				  {
					  sendHelpMessage(player);
					  return true;
				  }

				  int page = 1;
				  if(args.length > 2)
				  {
					  try
					  {
						  page = Integer.parseInt(args[2]);
						  if(page < 1)
						  {
							  page = 1;
						  }
					  }
					  catch(NumberFormatException e)
					  {
						  //well, page 1 it is then.
					  }
				  }
				  
				  @SuppressWarnings("deprecation")
				  OfflinePlayer objective = Bukkit.getOfflinePlayer(args[1]);
				  if(objective==null)
				  {
					  player.sendMessage("§6Could not find that player in our system!");
					  return true;
				  }
				  this.battle.inv.showStats(player, objective, page);
				  return true;
			  }			  
			  else if(args[0].equalsIgnoreCase("accept"))
			  {
				  //accept a duel.
				  if(args.length < 2)
				  {
					  //we need a playername.
					  return true;
				  }
				  else
				  {
					 p2 = Bukkit.getPlayer(args[1]);
					 //make sure player2 is online and exists.
					 if(p2 == null)
					  {
						  player.sendMessage("§cCould not find player §6" + args[0]);
						  return true;
					  }
					  if(!p2.isOnline())
					  {
						  player.sendMessage("§6" + args[0] + " §cis not online!");
						  return true;					  
					  }
					  
					  //get challenger from the hashmap.
					  Player check = duels.get(player);
					  if(check == p2)
					  {
						  duels.remove(player);
						  
						  //lets make sure none of these players are actually ingame.						  
						  if(this.battle.ingame.contains(player) || this.battle.ingame.contains(p2))
						  {
							  player.sendMessage(alreadyInGame);
							  p2.sendMessage(alreadyInGame);
							  return true;
						  }
						  //the numbers match up, lets battle!
						  battle.startBattle(player, p2);
					  }
					  else
					  {
						  player.sendMessage(this.nochallenge);
					  }
				  }
				  return true;
			  }
			  else if(args[0].equalsIgnoreCase("invite"))
			  {
				  if(args.length < 2)
				  {
					  this.sendHelpMessage(player);
					  return true;
				  }
				  p2 = Bukkit.getPlayer(args[1]);
				  
			  }
			  else
			  {
				  //args[0] was not any of the above, so we assume its a player.
				  p2 = Bukkit.getPlayer(args[0]);
			  }
				
			  //check if player2 is actually online and exists.
			  if(p2 == null)
			  {
				  player.sendMessage("§cCould not find player §6" + args[0]);
				  return true;
			  }
			  if(!p2.isOnline())
			  {
				  player.sendMessage("§6" + args[0] + " §cis not online!");
				  return true;					  
			  }
			  
			  if(p2==player)
			  {
				  //you sly dog, you cant battle yourself!
				  player.sendMessage(this.battleself);
				  return true;
			  }
			  
			  //if one of these players is already in game, stop it!
			  if(this.battle.ingame.contains(player) || this.battle.ingame.contains(p2))
			  {
				  player.sendMessage(alreadyInGame);
				  p2.sendMessage(alreadyInGame);
				  return true;
			  }
			  
			  //put p2-player pair in the hashmap, so we can accept it easily. this will put in (challenged, challenger).
			  duels.put(p2, player);
			  
			  //message player 2 that someone challenged them.
		  	  TextComponent textDuel = new TextComponent(this.duelInviteMessage.replace("<player>", player.getName()));
			  textDuel.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,  new ComponentBuilder(duelhovermessage).create() ));
			  textDuel.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel accept " + player.getName()));
			  p2.spigot().sendMessage(textDuel);
		  }
			  
		  
		return true;
	}
	
	public void sendHelpMessage(Player player)
	{
		for(String string: this.helpmessage)
		{
			player.sendMessage(string);
		}
	}
	
}
