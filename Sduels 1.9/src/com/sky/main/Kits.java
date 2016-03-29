package com.sky.main;

import java.util.LinkedHashMap;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Kits {

	public Kits(Main main)
	{
		this.loadKits(main.yml);
	}
	
	public static LinkedHashMap<String, Kit> kits = new LinkedHashMap<String, Kit>();
	
	public void loadKits(YamlConfiguration yml)
	{
		ConfigurationSection cs = yml.getConfigurationSection("kits");
		if(cs==null)
		{
			return;
		}
		for(String string: cs.getKeys(false))
		{
			String name = yml.getString("kits." + string + ".name").replace("&", "§");
			if(name==null)
			{
				continue;
			}
			Kits.kits.put(name, new Kit(yml, string));
		}
	}
	
	public void givePlayerKit(Player player, String kit)
	{
		Kit k = Kits.kits.get(kit);
		if(k.contents.isEmpty())
		{
			this.errorInventory(player);
			return;
		}
		for(ItemStack stack: k.contents)
		{
			String itemtype = stack.getType().toString().toLowerCase();
			if(itemtype.contains("chestplate") && player.getInventory().getChestplate()==null)
			{
				player.getInventory().setChestplate(stack);
				continue;
			}
			if(itemtype.contains("helmet") && player.getInventory().getHelmet()==null)
			{
				player.getInventory().setHelmet(stack);
				continue;
			}
			if(itemtype.contains("leggings") && player.getInventory().getLeggings()==null)
			{
				player.getInventory().setLeggings(stack);
				continue;
			}
			if(itemtype.contains("boots") && player.getInventory().getBoots()==null)
			{
				player.getInventory().setBoots(stack);
				continue;
			}
			player.getInventory().addItem(stack);
		}
		player.updateInventory();
	}
	
	public void errorInventory(Player player)
	{
		player.getInventory().clear();
		for(int i = 0; i < 36; i++)
		{
			player.getInventory().setItem(i, new ItemStack(Material.BEDROCK, -1));
		}
	}
	
}
