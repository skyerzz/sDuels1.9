package com.sky.main;

import java.io.File;
import java.util.HashMap;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class PlayerData
{
	public final String path;

	public int wins = 0, losses = 0;
	public boolean exists = false;

	public HashMap<String, Integer> previousDuels = new HashMap<String, Integer>();
	public HashMap<String, Integer> previousDuelWins = new HashMap<String, Integer>();
	
	public HashMap<String, Integer> kitwins = new HashMap<String, Integer>();
	public HashMap<String, Integer> kitloss = new HashMap<String, Integer>();
	
	private YamlConfiguration yml, newyml = new YamlConfiguration();
	

	public PlayerData(String path, OfflinePlayer player)
	{
		this.path = path;
		File file = FileManager.getFile(path);
		this.yml = YamlConfiguration.loadConfiguration(file);
		this.load();
	}

	
	public void load()
	{
		this.loadCommonData();
		this.loadPreviousDuels();
	}
	
	public void addPreviousDuel(String uuid, int amount)
	{
		if(!this.previousDuels.containsKey(uuid))
		{
			this.previousDuels.put(uuid, amount);
		}
		else
		{
			int prevamount = this.previousDuels.get(uuid);
			this.previousDuels.replace(uuid, prevamount + amount);
		}
	}

	public void addPreviousDuelWin(String uuid, int amount)
	{
		if(!this.previousDuelWins.containsKey(uuid))
		{
			this.previousDuelWins.put(uuid, amount);
		}
		else
		{
			int prevamount = this.previousDuelWins.get(uuid);
			this.previousDuelWins.replace(uuid, prevamount + amount);
		}
	}
	
	public void loadPreviousDuels()
	{
		ConfigurationSection cs = this.yml.getConfigurationSection("previousduels");
		if(cs==null)
		{
			return;
		}
		for(String string: cs.getKeys(false))
		{
			String[] battles = this.yml.getString("previousduels." + string).split(":");
			try
			{
				int previousduels = Integer.parseInt(battles[0]);
				this.addPreviousDuel(string, previousduels);
				int previousWins = Integer.parseInt(battles[1]);
				this.addPreviousDuelWin(string, previousWins);
			}
			catch(NumberFormatException | ArrayIndexOutOfBoundsException e)
			{
				continue;
			}
		}
	}
	
	public void loadCommonData()
	{
		String temp = this.yml.getString("wins");
		if(temp != null)
		{
			this.wins = this.yml.getInt("wins");
		}
		
		temp = this.yml.getString("losses");
		if(temp != null)
		{
			this.losses = this.yml.getInt("losses");
		}
		
		temp = this.yml.getString("exists");
		if(temp != null)
		{
			this.exists = true;
		}
		

		ConfigurationSection cs = yml.getConfigurationSection("kitstats");
		if(cs==null)
		{
			return;
		}
		
		for(String string: cs.getKeys(false))
		{
			temp = yml.getString("kitstats." + string + ".loss");
			if(temp!=null)
			{
				this.kitloss.put(string, yml.getInt("kitstats." + string + ".loss"));
			}
			
			temp = yml.getString("kitstats." + string + ".wins");
			if(temp!=null)
			{
				this.kitwins.put(string, yml.getInt("kitstats." + string + ".wins"));
			}
		}
		
		

	}

	public void saveCommonData()
	{
		this.newyml.set("wins", this.wins);
		this.newyml.set("losses", this.losses);
		this.newyml.set("exists", true);
		
		for(String string: kitloss.keySet())
		{
			this.newyml.set("kitstats." + string + "loss", kitloss.get(string));
		}
		
		for(String string: kitwins.keySet())
		{
			this.newyml.set("kitstats." + string + "wins", kitwins.get(string));
		}
	}
	
	public void save()
	{
		this.saveCommonData();
		this.savePreviousDuels();
		FileManager.saveFile(this.path, this.newyml);
	}

	public void savePreviousDuels()
	{
		for(String uuid: this.previousDuels.keySet())
		{
			this.newyml.set("previousduels." + uuid, this.previousDuels.get(uuid));
		}
	}

}